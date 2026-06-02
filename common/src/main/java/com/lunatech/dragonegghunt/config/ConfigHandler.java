package com.lunatech.dragonegghunt.config;

import com.lunatech.dragonegghunt.AbstractExample;
import com.lunatech.dragonegghunt.Reloadable;
import com.lunatech.dragonegghunt.config.loading.ConfigLoader;
import com.lunatech.dragonegghunt.config.typeserializer.StringListSerializer;
import com.lunatech.dragonegghunt.config.typeserializer.StringObjectMapSerializer;
import org.slf4j.Logger;

import java.nio.file.Path;

/**
 * A class that generates/loads {@literal &} provides access to a configuration file.
 */
public class ConfigHandler implements Reloadable {
    private final AbstractExample plugin;
    private final Path configDir;
    private final Logger logger;

    private PluginConfig cfg;
    private DatabaseConfig databaseCfg;
    private DashboardConfig dashboardCfg;

    /**
     * Instantiates a new Config handler.
     *
     * @param plugin the plugin instance
     */
    public ConfigHandler(AbstractExample plugin) {
        this.plugin = plugin;
        this.configDir = plugin.getDataFolder().toPath();
        this.logger = plugin.getComponentLogger();
    }

    public ConfigHandler(AbstractExample plugin, Path configDir, Logger logger) {
        this.plugin = plugin;
        this.configDir = configDir;
        this.logger = logger;
    }

    @Override
    public void onLoad(AbstractExample plugin) {
        cfg = new ConfigLoader()
            .withLogger(logger)
            .withDirectory()
            .withPath(configDir.resolve("config.yml"))
            .withHeader("")
            .build(PluginConfig.class);

        databaseCfg = new ConfigLoader()
            .withLogger(logger)
            .withDirectory()
            .withPath(configDir.resolve("database.yml"))
            .withHeader("")
            .withSerializer(b -> {
                b.registerExact(StringListSerializer.TYPE_TOKEN, StringListSerializer.INSTANCE)
                    .registerExact(StringObjectMapSerializer.TYPE_TOKEN, StringObjectMapSerializer.INSTANCE);
            })
            .build(DatabaseConfig.class);

        dashboardCfg = new ConfigLoader()
            .withLogger(logger)
            .withDirectory()
            .withPath(configDir.resolve("dashboard.yml"))
            .withHeader("Egg Hunt Admin Dashboard GUI configuration")
            .build(DashboardConfig.class);
    }

    /**
     * Validates that the configuration files can be successfully parsed.
     *
     * @return true if valid, false otherwise.
     */
    public boolean validateConfigs() {
        PluginConfig loadedCfg = new ConfigLoader()
            .withDirectory()
            .withPath(configDir.resolve("config.yml"))
            .withHeader("")
            .build(PluginConfig.class);

        DatabaseConfig loadedDbCfg = new ConfigLoader()
            .withDirectory()
            .withPath(configDir.resolve("database.yml"))
            .withHeader("")
            .withSerializer(b -> {
                b.registerExact(StringListSerializer.TYPE_TOKEN, StringListSerializer.INSTANCE)
                    .registerExact(StringObjectMapSerializer.TYPE_TOKEN, StringObjectMapSerializer.INSTANCE);
            })
            .build(DatabaseConfig.class);

        DashboardConfig loadedDashboardCfg = new ConfigLoader()
            .withDirectory()
            .withPath(configDir.resolve("dashboard.yml"))
            .withHeader("Egg Hunt Admin Dashboard GUI configuration")
            .build(DashboardConfig.class);

        return loadedCfg != null && loadedDbCfg != null && loadedDashboardCfg != null;
    }

    /**
     * Gets main config object.
     *
     * @return the config object
     */
    public PluginConfig getConfig() {
        return cfg;
    }

    /**
     * Gets database config object.
     *
     * @return the config object
     */
    public DatabaseConfig getDatabaseConfig() {
        return databaseCfg;
    }

    /**
     * Gets dashboard config object.
     *
     * @return the config object
     */
    public DashboardConfig getDashboardConfig() {
        return dashboardCfg;
    }
}
