package com.lunatech.dragonegghunt.command;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import com.lunatech.dragonegghunt.AbstractExample;
import com.lunatech.dragonegghunt.Example;
import com.lunatech.dragonegghunt.Reloadable;

/**
 * A class to handle registration of commands.
 */
public class CommandHandler implements Reloadable {
    public static final String BASE_PERM = "example.command";
    private final DragonEggHunt plugin;

    /**
     * Instantiates the Command handler.
     *
     * @param plugin the plugin
     */
    public CommandHandler(DragonEggHunt plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onLoad(AbstractExample plugin) {
        CommandAPI.onLoad(
            new CommandAPIPaperConfig(plugin)
                .silentLogs(true)
        );
    }

    @Override
    public void onEnable(AbstractExample plugin) {
        if (!CommandAPI.isLoaded())
            return;

        CommandAPI.onEnable();

        // Register commands here
        new ExampleCommand(plugin)
            .command()
            .withAliases()
            .register();

        new EggHuntCommand(this.plugin)
            .command()
            .register();

    }

    @Override
    public void onDisable(AbstractExample plugin) {
        if (!CommandAPI.isLoaded())
            return;

        CommandAPI.onDisable();
    }
}