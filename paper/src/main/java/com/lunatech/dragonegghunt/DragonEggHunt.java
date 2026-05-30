package com.lunatech.dragonegghunt;

import com.lunatech.dragonegghunt.api.ExampleAPI;
import com.lunatech.dragonegghunt.command.CommandHandler;
import com.lunatech.dragonegghunt.config.ConfigHandler;
import com.lunatech.dragonegghunt.cooldown.CooldownHandler;
import com.lunatech.dragonegghunt.database.handler.DatabaseHandler;
import com.lunatech.dragonegghunt.hook.HookManager;
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
public class DragonEggHunt extends AbstractExample {
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
    private space.arim.morepaperlib.scheduling.ScheduledTask broadcastTask;


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
            cooldownHandler,
            apiHandler
        );

        DB.init(databaseHandler);
        Messaging.init(messagingHandler);

        // Initialize Egg tracker state & repository
        com.lunatech.dragonegghunt.persistence.EggStateRepository eggRepository = new com.lunatech.dragonegghunt.persistence.impl.SqlEggStateRepository();
        this.eggTrackerService = new com.lunatech.dragonegghunt.service.impl.DefaultEggTrackerService(eggRepository);


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

        // Load egg tracker state
        eggTrackerService.loadState();
        eggTrackerService.setOverrideRegionProtection(configHandler.getConfig().dragonEggTracker.overrideRegionProtection);

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
    }

    @Override
    public void onDisable() {
        if (broadcastTask != null) {
            broadcastTask.cancel();
        }
        for (Reloadable handler : handlers.reversed()) // If reverse doesn't work implement a new List with your desired disable order
            handler.onDisable(instance);
    }

    /**
     * Use to reload the entire plugin.
     */
    public void onReload() {
        onDisable();
        onLoad();
        onEnable();
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

    public @NotNull ExampleAPI getApiHandler() {
        return apiHandler;
    }

    @Override
    public @NotNull com.lunatech.dragonegghunt.service.EggTrackerService getEggTrackerService() {
        return eggTrackerService;
    }
}
