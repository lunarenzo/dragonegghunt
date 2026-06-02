package com.lunatech.dragonegghunt.hook.bstats;

import com.lunatech.dragonegghunt.AbstractPlugin;
import com.lunatech.dragonegghunt.DragonEggHunt;
import com.lunatech.dragonegghunt.hook.AbstractHook;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * A hook to interface with <a href="https://github.com/Bastian/bstats-metrics">BStats</a>.
 */
public class BStatsHook extends AbstractHook {
    private final static int BSTATS_ID = 31754;
    private @Nullable Metrics hook;

    /**
     * Instantiates a new BStats hook.
     *
     * @param plugin the plugin instance
     */
    public BStatsHook(DragonEggHunt plugin) {
        super(plugin);
    }

    @Override
    public void onEnable(AbstractPlugin plugin) {
        // Catch startup errors for bstats
        try {
            Metrics metrics = new Metrics(getPlugin(), BSTATS_ID);

            // Add custom charts to report useful configuration stats
            metrics.addCustomChart(new SimplePie("tracking_method", () -> {
                try {
                    return getPlugin().getConfigHandler().getConfig().dragonEggTracker.trackingMethod;
                } catch (Exception e) {
                    return "BOTH";
                }
            }));

            metrics.addCustomChart(new SimplePie("override_region_protection", () -> {
                try {
                    return String.valueOf(getPlugin().getConfigHandler().getConfig().dragonEggTracker.overrideRegionProtection);
                } catch (Exception e) {
                    return "false";
                }
            }));

            metrics.addCustomChart(new SimplePie("allow_container_storage", () -> {
                try {
                    return String.valueOf(getPlugin().getConfigHandler().getConfig().dragonEggTracker.allowContainerStorage);
                } catch (Exception e) {
                    return "false";
                }
            }));

            setHook(metrics);
        } catch (Exception ignored) {
            setHook(null);
        }
    }

    @Override
    public void onDisable(AbstractPlugin plugin) {
        getHook().shutdown();
        setHook(null);
    }

    @Override
    public boolean isHookLoaded() {
        return hook != null;
    }

    /**
     * Gets BStats metrics instance. Should only be used following {@link #isHookLoaded()}.
     *
     * @return instance
     */
    public Metrics getHook() {
        if (!isHookLoaded())
            throw new IllegalStateException("Attempted to access BStats metrics instance hook when it is unavailable!");

        return hook;
    }

    /**
     * Sets the BStats metrics instance.
     *
     * @param hook The BStats metrics instance {@link Metrics}
     */
    @ApiStatus.Internal
    private void setHook(@Nullable Metrics hook) {
        this.hook = hook;
    }
}
