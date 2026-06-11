package com.lunatech.dragonegghunt.hook.pvpmanager;

import com.lunatech.dragonegghunt.AbstractPlugin;
import com.lunatech.dragonegghunt.DragonEggHunt;
import com.lunatech.dragonegghunt.hook.AbstractHook;
import com.lunatech.dragonegghunt.service.CombatSessionService;
import com.lunatech.dragonegghunt.service.EggTrackerService;
import me.chancesd.pvpmanager.PvPManager;
import me.chancesd.pvpmanager.integration.Hook;
import me.chancesd.pvpmanager.integration.type.ForceToggleDependency;
import me.chancesd.pvpmanager.manager.DependencyManager;
import me.chancesd.pvpmanager.player.ProtectionType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Hook to interface with PvPManager to coordinate combat sessions and override region/newbie protection.
 */
public final class PvPManagerHook extends AbstractHook implements ForceToggleDependency, Listener {

    private final @NotNull EggTrackerService eggTrackerService;
    private final @NotNull CombatSessionService combatSessionService;
    private boolean loaded = false;

    /**
     * Instantiates a new PvPManager hook.
     *
     * @param plugin the plugin instance
     */
    public PvPManagerHook(@NotNull DragonEggHunt plugin) {
        super(plugin);
        this.eggTrackerService = plugin.getEggTrackerService();
        this.combatSessionService = plugin.getCombatSessionService();
    }

    @Override
    public boolean isHookLoaded() {
        return loaded;
    }

    @Override
    public void onEnable(@NotNull AbstractPlugin plugin) {
        if (isPluginEnabled("PvPManager")) {
            try {
                final Plugin pvpManagerPlugin = Bukkit.getPluginManager().getPlugin("PvPManager");
                if (pvpManagerPlugin instanceof PvPManager pm) {
                    final DependencyManager dm = pm.getDependencyManager();
                    final Field field = DependencyManager.class.getDeclaredField("togglePvPChecks");
                    field.setAccessible(true);
                    
                    @SuppressWarnings("unchecked")
                    final List<ForceToggleDependency> list = (List<ForceToggleDependency>) field.get(dm);
                    list.add(this);
                    
                    loaded = true;
                    getPlugin().getSLF4JLogger().info("PvPManager integration hook enabled and injected successfully.");
                }
            } catch (Throwable t) {
                getPlugin().getSLF4JLogger().error("Failed to inject PvPManager hook", t);
            }
        }
    }

    @Override
    public void onDisable(@NotNull AbstractPlugin plugin) {
        if (loaded) {
            try {
                final Plugin pvpManagerPlugin = Bukkit.getPluginManager().getPlugin("PvPManager");
                if (pvpManagerPlugin instanceof PvPManager pm) {
                    final DependencyManager dm = pm.getDependencyManager();
                    final Field field = DependencyManager.class.getDeclaredField("togglePvPChecks");
                    field.setAccessible(true);
                    
                    @SuppressWarnings("unchecked")
                    final List<ForceToggleDependency> list = (List<ForceToggleDependency>) field.get(dm);
                    list.remove(this);
                    
                    getPlugin().getSLF4JLogger().info("PvPManager integration hook disabled and removed successfully.");
                }
            } catch (Throwable t) {
                getPlugin().getSLF4JLogger().error("Failed to remove PvPManager hook during disable", t);
            }
            loaded = false;
        }
    }

    @Override
    public @NotNull String getName() {
        return "DragonEggHunt";
    }

    @Override
    public @Nullable Hook getHook() {
        return null; // Not registered in the main dependencies map, so null is safe.
    }

    @Override
    public @NotNull JavaPlugin getPlugin() {
        return getPlugin();
    }

    @Override
    public boolean shouldDisable(@NotNull Player player) {
        if (!isHookLoaded()) {
            return false;
        }
        if (!eggTrackerService.isOverrideRegionProtection()) {
            return false;
        }
        return eggTrackerService.isPvpForced(player.getUniqueId());
    }

    @Override
    public boolean shouldDisable(@NotNull Player attacker, @NotNull Player defender, @Nullable ProtectionType reason) {
        if (!isHookLoaded()) {
            return false;
        }
        if (!eggTrackerService.isOverrideRegionProtection()) {
            return false;
        }
        final boolean isAttackerHolder = eggTrackerService.isPvpForced(attacker.getUniqueId());
        final boolean isDefenderHolder = eggTrackerService.isPvpForced(defender.getUniqueId());
        return isAttackerHolder || isDefenderHolder;
    }

    @Override
    public boolean shouldDisableProtection() {
        return true;
    }
}
