package com.lunatech.dragonegghunt.service.impl;

import com.lunatech.dragonegghunt.DragonEggHunt;
import com.lunatech.dragonegghunt.service.DeathHandlingService;
import com.lunatech.dragonegghunt.state.EggState;
import com.lunatech.dragonegghunt.utility.EggItemFactory;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link DeathHandlingService} that handles removing the Alpha Dragon Egg
 * from the dying player's drops and inventory, dropping it physically, and updating the state.
 */
public final class DeathHandlingServiceImpl implements DeathHandlingService {

    private final DragonEggHunt plugin;

    /**
     * Instantiates a new Death handling service.
     *
     * @param plugin the plugin instance
     */
    public DeathHandlingServiceImpl(@NotNull DragonEggHunt plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handleHolderDeath(@NotNull PlayerDeathEvent event) {
        Player player = event.getEntity();
        boolean eggFound = false;

        // 1. Purge the Alpha Dragon Egg from the death drops list
        List<ItemStack> drops = event.getDrops();
        if (drops != null && !drops.isEmpty()) {
            int initialSize = drops.size();
            drops.removeIf(EggItemFactory::isAlphaEgg);
            if (drops.size() < initialSize) {
                eggFound = true;
            }
        }

        // 2. Purge the Alpha Dragon Egg from the player's inventory
        // (Necessary for plugins that scrape/override keepInventory)
        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (EggItemFactory.isAlphaEgg(contents[i])) {
                contents[i] = null;
                eggFound = true;
            }
        }
        inventory.setContents(contents);

        // Purge from offhand
        ItemStack offHand = inventory.getItemInOffHand();
        if (EggItemFactory.isAlphaEgg(offHand)) {
            inventory.setItemInOffHand(null);
            eggFound = true;
        }

        // Purge from player's cursor
        org.bukkit.inventory.InventoryView openInv = player.getOpenInventory();
        if (openInv != null) {
            ItemStack cursor = openInv.getCursor();
            if (EggItemFactory.isAlphaEgg(cursor)) {
                openInv.setCursor(null);
                eggFound = true;
            }

            // Purge from active crafting grids or workbench inventories
            org.bukkit.inventory.Inventory topInventory = openInv.getTopInventory();
            if (topInventory != null) {
                org.bukkit.event.inventory.InventoryType type = topInventory.getType();
                if (type == org.bukkit.event.inventory.InventoryType.CRAFTING ||
                    type == org.bukkit.event.inventory.InventoryType.WORKBENCH) {
                    ItemStack[] topContents = topInventory.getContents();
                    for (int i = 0; i < topContents.length; i++) {
                        if (EggItemFactory.isAlphaEgg(topContents[i])) {
                            topContents[i] = null;
                            eggFound = true;
                        }
                    }
                    topInventory.setContents(topContents);
                }
            }
        }

        // 3. Resolve the current tracker state to verify if this player is the registered holder
        EggState state = plugin.getEggTrackerService().getState();
        UUID trackedHolder = null;
        if (state instanceof EggState.Held held) {
            trackedHolder = held.holderUuid();
        }

        // 4. If the egg was found in possession, or if this player was the registered holder, drop it physically
        if (eggFound || player.getUniqueId().equals(trackedHolder)) {
            Location loc = player.getLocation();
            ItemStack alphaEgg = EggItemFactory.createAlphaEgg(plugin);
            Item itemEntity = player.getWorld().dropItemNaturally(loc, alphaEgg);

            // Tag the item entity with the ALPHA_EGG_KEY
            itemEntity.getPersistentDataContainer().set(
                DragonEggHunt.ALPHA_EGG_KEY,
                org.bukkit.persistence.PersistentDataType.INTEGER,
                1
            );

            // Transition the tracker state to Dropped
            plugin.getEggTrackerService().updateState(new EggState.Dropped(
                loc.getWorld().getName(),
                loc.getX(),
                loc.getY(),
                loc.getZ(),
                itemEntity.getUniqueId()
            ));
        }
    }
}
