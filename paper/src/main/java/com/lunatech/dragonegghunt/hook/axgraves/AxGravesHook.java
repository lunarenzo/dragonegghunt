package com.lunatech.dragonegghunt.hook.axgraves;

import com.artillexstudios.axgraves.api.events.GraveSpawnEvent;
import com.artillexstudios.axgraves.grave.Grave;
import com.lunatech.dragonegghunt.AbstractPlugin;
import com.lunatech.dragonegghunt.DragonEggHunt;
import com.lunatech.dragonegghunt.hook.AbstractHook;
import com.lunatech.dragonegghunt.utility.EggItemFactory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Type-safe, compile-time hook for the AxGraves plugin.
 * Extends AbstractHook and listens to GraveSpawnEvent to safely scrub
 * duplicate Alpha Dragon Eggs from graves on player death.
 */
public final class AxGravesHook extends AbstractHook implements Listener {

    private boolean loaded = false;

    /**
     * Instantiates a new AxGraves hook.
     *
     * @param plugin the plugin instance
     */
    public AxGravesHook(DragonEggHunt plugin) {
        super(plugin);
    }

    @Override
    public boolean isHookLoaded() {
        return loaded;
    }

    @Override
    public void onEnable(AbstractPlugin plugin) {
        if (isPluginEnabled("AxGraves")) {
            loaded = true;
            getPlugin().getSLF4JLogger().info("AxGraves integration hook enabled.");
        }
    }

    /**
     * Listens to the GraveSpawnEvent from AxGraves.
     * Searches the spawned grave inventory for any Alpha Dragon Egg and removes it.
     *
     * @param event the GraveSpawnEvent
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onGraveSpawn(GraveSpawnEvent event) {
        Grave grave = event.getGrave();
        if (grave == null) {
            return;
        }

        Inventory gui = grave.getGui();
        if (gui == null) {
            return;
        }

        ItemStack[] contents = gui.getContents();
        boolean modified = false;
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (EggItemFactory.isAlphaEgg(item)) {
                contents[i] = null;
                modified = true;
            }
        }

        if (modified) {
            gui.setContents(contents);
            getPlugin().getSLF4JLogger().info("Successfully scrubbed duplicate Alpha Dragon Egg from Grave GUI.");
        }
    }
}
