package io.github.exampleuser.example;

import io.github.exampleuser.example.api.ExampleAPI;
import io.github.exampleuser.example.command.CommandHandler;
import io.github.exampleuser.example.config.ConfigHandler;
import io.github.exampleuser.example.cooldown.CooldownHandler;
import io.github.exampleuser.example.database.handler.DatabaseHandler;
import io.github.exampleuser.example.hook.HookManager;
import io.github.exampleuser.example.listener.ListenerHandler;
import io.github.exampleuser.example.messaging.MessagingHandler;
import io.github.exampleuser.example.threadutil.SchedulerHandler;
import io.github.exampleuser.example.translation.TranslationHandler;
import io.github.exampleuser.example.updatechecker.UpdateHandler;
import io.github.exampleuser.example.utility.DB;
import io.github.exampleuser.example.utility.Logger;
import io.github.exampleuser.example.utility.Messaging;
import io.github.milkdrinkers.colorparser.paper.ColorParser;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Main class.
 */
@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class Example extends AbstractExample {
    private static Example instance;

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
    private ExampleAPIProvider apiHandler;

    // Handlers list (defines order of load/enable/disable)
    private List<? extends Reloadable> handlers;

    private io.github.exampleuser.example.service.EggTrackerService eggTrackerService;
    private space.arim.morepaperlib.scheduling.ScheduledTask broadcastTask;


    @Override
    public void onLoad() {
        instance = this;

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
        apiHandler = new ExampleAPIProvider(this);

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
        io.github.exampleuser.example.persistence.EggStateRepository eggRepository = new io.github.exampleuser.example.persistence.impl.SqlEggStateRepository();
        this.eggTrackerService = new io.github.exampleuser.example.service.impl.DefaultEggTrackerService(eggRepository);


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
                new io.github.exampleuser.example.task.TrackerBroadcastTask(this),
                20L,
                interval
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
    public @NotNull io.github.exampleuser.example.service.EggTrackerService getEggTrackerService() {
        return eggTrackerService;
    }
}
