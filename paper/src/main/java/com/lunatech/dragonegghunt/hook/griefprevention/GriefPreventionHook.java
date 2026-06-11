package com.lunatech.dragonegghunt.hook.griefprevention;

import com.lunatech.dragonegghunt.AbstractPlugin;
import com.lunatech.dragonegghunt.DragonEggHunt;
import com.lunatech.dragonegghunt.hook.AbstractHook;
import com.lunatech.dragonegghunt.hook.ClaimProvider;
import com.lunatech.dragonegghunt.hook.Hook;
import me.ryanhamshire.GriefPrevention.events.PreventPvPEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

/**
 * A hook to interface with <a href="https://github.com/TechFortress/GriefPrevention">GriefPrevention</a>.
 */
public class GriefPreventionHook extends AbstractHook implements ClaimProvider, Listener {

    private boolean loaded = false;

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
        return loaded;
    }

    @Override
    public void onEnable(AbstractPlugin plugin) {
        if (isPluginEnabled("GriefPrevention")) {
            loaded = true;
            getPlugin().getSLF4JLogger().info("GriefPrevention integration hook enabled.");
        }
    }

    /**
     * Intercepts GriefPrevention's PvP check event to bypass claim-based PvP restrictions for egg holder and hunters.
     *
     * @param event the PreventPvPEvent
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPreventPvP(PreventPvPEvent event) {
        if (!isHookLoaded()) {
            return;
        }

        if (!(event.getDefender() instanceof Player victim)) {
            return;
        }

        Player damager = event.getAttacker();
        if (damager == null) {
            return;
        }

        boolean isVictimHolder = getPlugin().getEggTrackerService().isPvpForced(victim.getUniqueId());
        boolean isDamagerHolder = getPlugin().getEggTrackerService().isPvpForced(damager.getUniqueId());

        if (!isVictimHolder && !isDamagerHolder) {
            return; // Neither player is the egg holder
        }

        // Only override region protection if overrideRegionProtection is enabled in config
        if (!getPlugin().getEggTrackerService().isOverrideRegionProtection()) {
            return;
        }

        // Always cancel GriefPrevention's check to give DragonEggHunt sole authority over holder PvP rules
        event.setCancelled(true);
    }
}
