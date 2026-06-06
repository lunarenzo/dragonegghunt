package com.lunatech.dragonegghunt;

import com.lunatech.dragonegghunt.api.DragonEggHuntAPI;
import com.lunatech.dragonegghunt.command.CommandHandler;
import com.lunatech.dragonegghunt.config.ConfigHandler;
import com.lunatech.dragonegghunt.cooldown.CooldownHandler;
import com.lunatech.dragonegghunt.database.handler.DatabaseHandler;
import com.lunatech.dragonegghunt.hook.ClaimProvider;
import com.lunatech.dragonegghunt.hook.Hook;
import com.lunatech.dragonegghunt.hook.HookManager;
import com.lunatech.dragonegghunt.hook.impl.NoOpClaimProvider;
import com.lunatech.dragonegghunt.listener.ListenerHandler;
import com.lunatech.dragonegghunt.messaging.MessagingHandler;
import com.lunatech.dragonegghunt.threadutil.SchedulerHandler;
import com.lunatech.dragonegghunt.translation.TranslationHandler;
import com.lunatech.dragonegghunt.updatechecker.UpdateHandler;
import com.lunatech.dragonegghunt.utility.DB;
import com.lunatech.dragonegghunt.utility.Logger;
import com.lunatech.dragonegghunt.utility.Messaging;
import io.github.milkdrinkers.colorparser.paper.ColorParser;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Main class.
 */
@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class DragonEggHunt extends AbstractPlugin {
    private static DragonEggHunt instance;

    // Handlers/Managers
    private ConfigHandler configHandler;
    private TranslationHandler translationHandler;
    private DatabaseHandler databaseHandler;
    private MessagingHandler messagingHandler;
    private HookManager hookManager;
    private CommandHandler commandHandler;
    private ListenerHandler listenerHandler;
    private UpdateHandler updateHandler;
    private SchedulerHandler schedulerHandler;
    private CooldownHandler cooldownHandler;
    private DragonEggHuntAPIProvider apiHandler;

    // Handlers list (defines order of load/enable/disable)
    private List<? extends Reloadable> handlers;

    private com.lunatech.dragonegghunt.service.EggTrackerService eggTrackerService;
    private com.lunatech.dragonegghunt.service.EggAuditService eggAuditService;
    private com.lunatech.dragonegghunt.service.TrackerRecipeService trackerRecipeService;
    private com.lunatech.dragonegghunt.service.AltarService altarService;
    private com.lunatech.dragonegghunt.service.ClaimValidationService claimValidationService;
    private space.arim.morepaperlib.scheduling.ScheduledTask broadcastTask;
    private space.arim.morepaperlib.scheduling.ScheduledTask potionBuffTask;
    private com.lunatech.dragonegghunt.task.PotionBuffTask potionBuffTaskRunner;
    private space.arim.morepaperlib.scheduling.ScheduledTask databasePruneTask;


    public static org.bukkit.NamespacedKey ALPHA_EGG_KEY;

    @Override
    public void onLoad() {
        instance = this;
        ALPHA_EGG_KEY = new org.bukkit.NamespacedKey(this, "alpha_egg");

        configHandler = new ConfigHandler(this);
        translationHandler = new TranslationHandler(configHandler);
        databaseHandler = DatabaseHandler.builder()
            .withConfigHandler(configHandler)
            .withLogger(getComponentLogger())
            .withMigrate(true)
            .build();
        messagingHandler = MessagingHandler.builder()
            .withLogger(getComponentLogger())
            .withName(getName())
            .build();
        hookManager = new HookManager(this);
        commandHandler = new CommandHandler(this);
        listenerHandler = new ListenerHandler(this);
        updateHandler = new UpdateHandler(this);
        schedulerHandler = new SchedulerHandler();
        cooldownHandler = new CooldownHandler();
        apiHandler = new DragonEggHuntAPIProvider(this);

        handlers = List.of(
            configHandler,
            translationHandler,
            databaseHandler,
            messagingHandler,
            hookManager,
            commandHandler,
            listenerHandler,
            updateHandler,
            schedulerHandler,
            cooldownHandler,
            apiHandler
        );

        DB.init(databaseHandler);
        Messaging.init(messagingHandler);

        // Initialize Egg tracker state & repository
        com.lunatech.dragonegghunt.persistence.EggStateRepository eggRepository = new com.lunatech.dragonegghunt.persistence.impl.SqlEggStateRepository();
        this.eggTrackerService = new com.lunatech.dragonegghunt.service.impl.DefaultEggTrackerService(eggRepository);
        this.trackerRecipeService = new com.lunatech.dragonegghunt.service.impl.TrackerRecipeServiceImpl(this);

        // Initialize Egg audit logs state & repository
        com.lunatech.dragonegghunt.persistence.AuditLogRepository auditRepository = new com.lunatech.dragonegghunt.persistence.impl.SqlAuditLogRepository();
        this.eggAuditService = new com.lunatech.dragonegghunt.service.impl.DefaultEggAuditService(auditRepository);
        this.altarService = new com.lunatech.dragonegghunt.service.impl.DefaultAltarService(this);
        this.claimValidationService = new com.lunatech.dragonegghunt.service.impl.ClaimValidationServiceImpl(this);


        for (Reloadable handler : handlers)
            handler.onLoad(instance);
    }

    @Override
    public void onEnable() {
        for (Reloadable handler : handlers)
            handler.onEnable(instance);

        if (!DB.isStarted()) {
            Logger.get().warn(ColorParser.of("<yellow>Database handler failed to start. Database support has been disabled.").build());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (!Messaging.isReady() && configHandler.getDatabaseConfig().messaging.enabled) {
            Logger.get().warn(ColorParser.of("<yellow>Messaging handler failed to start. Messaging support has been disabled.").build());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Start audit service logger (safe after DB start)
        if (eggAuditService != null) {
            eggAuditService.start();
        }

        // Load egg tracker state
        eggTrackerService.loadState();
        eggTrackerService.setOverrideRegionProtection(configHandler.getConfig().dragonEggTracker.overrideRegionProtection);

        // Perform initial claim validation sweep on startup
        if (claimValidationService != null) {
            claimValidationService.validateEggLocation();
        }

        // Generate the altar pedestal at startup if configured and world is loaded
        if (altarService != null) {
            altarService.generateAltarPedestal();
        }

        // Register state listener for audit logs
        eggTrackerService.setStateListener(new com.lunatech.dragonegghunt.listener.AuditEventDispatcher(this));

        // Register custom compass tracker recipe
        if (trackerRecipeService != null) {
            trackerRecipeService.registerRecipe();
        }

        // Start repeating broadcast task
        int interval = configHandler.getConfig().dragonEggTracker.broadcastInterval;
        if (interval > 0) {
            space.arim.morepaperlib.MorePaperLib morePaperLib = new space.arim.morepaperlib.MorePaperLib(this);
            broadcastTask = morePaperLib.scheduling().asyncScheduler().runAtFixedRate(
                new com.lunatech.dragonegghunt.task.TrackerBroadcastTask(this),
                java.time.Duration.ofMillis(20L * 50L),
                java.time.Duration.ofMillis(interval * 50L)
            );
        }

        // Start repeating potion buff task
        com.lunatech.dragonegghunt.config.PluginConfig.PotionBuffReward rewardConfig = configHandler.getConfig().dragonEggTracker.potionBuffReward;
        if (rewardConfig != null && rewardConfig.enabled) {
            potionBuffTaskRunner = new com.lunatech.dragonegghunt.task.PotionBuffTask(this);
            space.arim.morepaperlib.MorePaperLib morePaperLib = new space.arim.morepaperlib.MorePaperLib(this);
            potionBuffTask = morePaperLib.scheduling().asyncScheduler().runAtFixedRate(
                potionBuffTaskRunner,
                java.time.Duration.ofMillis(20L * 50L),
                java.time.Duration.ofMillis(20L * 50L)
            );
        }

        // Start repeating database prune task (runs every 6 hours)
        space.arim.morepaperlib.MorePaperLib morePaperLib = new space.arim.morepaperlib.MorePaperLib(this);
        databasePruneTask = morePaperLib.scheduling().asyncScheduler().runAtFixedRate(
            new com.lunatech.dragonegghunt.task.DatabasePruneTask(this),
            java.time.Duration.ofMillis(0), // run immediately on startup
            java.time.Duration.ofHours(6)   // run every 6 hours
        );
    }

    @Override
    public void onDisable() {
        if (broadcastTask != null) {
            broadcastTask.cancel();
        }
        if (potionBuffTask != null) {
            potionBuffTask.cancel();
            potionBuffTask = null;
        }
        if (potionBuffTaskRunner != null) {
            potionBuffTaskRunner.cleanup();
            potionBuffTaskRunner = null;
        }
        if (databasePruneTask != null) {
            databasePruneTask.cancel();
            databasePruneTask = null;
        }
        if (eggAuditService != null) {
            eggAuditService.flushQueueSync();
        }
        if (trackerRecipeService != null) {
            trackerRecipeService.unregisterRecipe();
        }
        for (Reloadable handler : handlers.reversed()) // If reverse doesn't work implement a new List with your desired disable order
            handler.onDisable(instance);
    }

    /**
     * Reload only the plugin configuration file.
     */
    public void reloadConfigOnly() {
        if (configHandler != null && !configHandler.validateConfigs()) {
            throw new IllegalArgumentException("Configuration file contains syntax or validation errors. Reload aborted.");
        }
        if (configHandler != null) {
            configHandler.onLoad(this);
        }
        if (eggTrackerService != null && configHandler != null) {
            eggTrackerService.setOverrideRegionProtection(configHandler.getConfig().dragonEggTracker.overrideRegionProtection);
        }
        if (trackerRecipeService != null) {
            trackerRecipeService.registerRecipe();
        }
        if (configHandler != null) {
            if (broadcastTask != null) {
                broadcastTask.cancel();
                broadcastTask = null;
            }
            int interval = configHandler.getConfig().dragonEggTracker.broadcastInterval;
            if (interval > 0) {
                space.arim.morepaperlib.MorePaperLib morePaperLib = new space.arim.morepaperlib.MorePaperLib(this);
                broadcastTask = morePaperLib.scheduling().asyncScheduler().runAtFixedRate(
                    new com.lunatech.dragonegghunt.task.TrackerBroadcastTask(this),
                    java.time.Duration.ofMillis(20L * 50L),
                    java.time.Duration.ofMillis(interval * 50L)
                );
            }

            if (potionBuffTask != null) {
                potionBuffTask.cancel();
                potionBuffTask = null;
            }
            if (potionBuffTaskRunner != null) {
                potionBuffTaskRunner.cleanup();
                potionBuffTaskRunner = null;
            }
            com.lunatech.dragonegghunt.config.PluginConfig.PotionBuffReward rewardConfig = configHandler.getConfig().dragonEggTracker.potionBuffReward;
            if (rewardConfig != null && rewardConfig.enabled) {
                potionBuffTaskRunner = new com.lunatech.dragonegghunt.task.PotionBuffTask(this);
                space.arim.morepaperlib.MorePaperLib morePaperLib = new space.arim.morepaperlib.MorePaperLib(this);
                potionBuffTask = morePaperLib.scheduling().asyncScheduler().runAtFixedRate(
                    potionBuffTaskRunner,
                    java.time.Duration.ofMillis(20L * 50L),
                    java.time.Duration.ofMillis(20L * 50L)
                );
            }

            if (databasePruneTask != null) {
                databasePruneTask.cancel();
                databasePruneTask = null;
            }
            space.arim.morepaperlib.MorePaperLib morePaperLib = new space.arim.morepaperlib.MorePaperLib(this);
            databasePruneTask = morePaperLib.scheduling().asyncScheduler().runAtFixedRate(
                new com.lunatech.dragonegghunt.task.DatabasePruneTask(this),
                java.time.Duration.ofMillis(0),
                java.time.Duration.ofHours(6)
            );
        }
    }

    /**
     * Reload only the translation language files.
     */
    public void reloadLangOnly() {
        if (configHandler != null && configHandler.getConfig() != null) {
            io.github.milkdrinkers.wordweaver.Translation.setLanguage(configHandler.getConfig().language);
        }
        io.github.milkdrinkers.wordweaver.Translation.reload();
    }

    /**
     * Reload database connections and messaging setup.
     */
    public void reloadDatabaseOnly() {
        if (eggAuditService != null) {
            eggAuditService.flushQueueSync();
        }
        if (databaseHandler != null) {
            databaseHandler.onDisable(this);
            databaseHandler.onLoad(this);
        }
        if (messagingHandler != null) {
            messagingHandler.onDisable(this);
            messagingHandler.onLoad(this);
            messagingHandler.onEnable(this);
        }
        if (eggAuditService != null) {
            eggAuditService.start();
        }
    }

    /**
     * Use to reload the entire plugin.
     */
    public void onReload() {
        if (configHandler != null && !configHandler.validateConfigs()) {
            throw new IllegalArgumentException("Configuration file contains syntax or validation errors. Reload aborted.");
        }
        try {
            Logger.get().info(ColorParser.of("<green>Reloading DragonEggHunt...").build());
            reloadConfigOnly();
            reloadLangOnly();
            reloadDatabaseOnly();
            Logger.get().info(ColorParser.of("<green>DragonEggHunt reloaded successfully.").build());
        } catch (Throwable t) {
            Logger.get().error(ColorParser.of("<red>Failed to reload DragonEggHunt! Safely disabling to prevent server crash or memory leak...").build());
            t.printStackTrace();
            try {
                onDisable();
            } catch (Throwable disableEx) {
                // Suppress secondary disable exceptions
            }
            Bukkit.getPluginManager().disablePlugin(this);
            throw new RuntimeException("Plugin reload failed: " + t.getMessage(), t);
        }
    }

    @Override
    public @NotNull ConfigHandler getConfigHandler() {
        return configHandler;
    }

    public @NotNull HookManager getHookManager() {
        return hookManager;
    }

    public @NotNull UpdateHandler getUpdateHandler() {
        return updateHandler;
    }

    public @NotNull DragonEggHuntAPI getApiHandler() {
        return apiHandler;
    }

    @Override
    public @NotNull com.lunatech.dragonegghunt.service.EggTrackerService getEggTrackerService() {
        return eggTrackerService;
    }

    public @NotNull com.lunatech.dragonegghunt.service.EggAuditService getEggAuditService() {
        return eggAuditService;
    }

    public @NotNull com.lunatech.dragonegghunt.service.TrackerRecipeService getTrackerRecipeService() {
        return trackerRecipeService;
    }

    @Override
    public @NotNull com.lunatech.dragonegghunt.service.AltarService getAltarService() {
        return altarService;
    }

    /**
     * Gets the active claim provider.
     *
     * @return the claim provider
     */
    public @NotNull ClaimProvider getClaimProvider() {
        if (Hook.GriefPrevention.isLoaded()) {
            return (ClaimProvider) Hook.GriefPrevention.get();
        }
        return NoOpClaimProvider.INSTANCE;
    }

    public @NotNull com.lunatech.dragonegghunt.service.ClaimValidationService getClaimValidationService() {
        return claimValidationService;
    }
}
