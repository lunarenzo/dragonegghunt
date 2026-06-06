package com.lunatech.dragonegghunt.hook.griefprevention;

import com.lunatech.dragonegghunt.AbstractPlugin;
import com.lunatech.dragonegghunt.DragonEggHunt;
import com.lunatech.dragonegghunt.hook.AbstractHook;
import com.lunatech.dragonegghunt.hook.ClaimProvider;
import com.lunatech.dragonegghunt.hook.Hook;
import org.bukkit.Location;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

/**
 * A hook to interface with <a href="https://github.com/TechFortress/GriefPrevention">GriefPrevention</a>.
 */
public class GriefPreventionHook extends AbstractHook implements ClaimProvider {

    /**
     * Instantiates a new GriefPrevention hook.
     *
     * @param plugin the plugin instance
     */
    public GriefPreventionHook(DragonEggHunt plugin) {
        super(plugin);
    }

    @Override
    public boolean isInClaim(Location location) {
        if (!isHookLoaded()) {
            return false;
        }
        try {
            if (GriefPrevention.instance == null || GriefPrevention.instance.dataStore == null) {
                return false;
            }
            return GriefPrevention.instance.dataStore.getClaimAt(location, true, null) != null;
        } catch (Throwable t) {
            getPlugin().getSLF4JLogger().warn("Failed to check GriefPrevention claim at location: " + location, t);
            return false;
        }
    }

    @Override
    public boolean isHookLoaded() {
        return isPluginPresent("GriefPrevention") && isPluginEnabled("GriefPrevention");
    }
}
