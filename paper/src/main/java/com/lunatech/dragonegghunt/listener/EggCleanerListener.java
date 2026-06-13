package com.lunatech.dragonegghunt.listener;

import com.lunatech.dragonegghunt.DragonEggHunt;
import com.lunatech.dragonegghunt.service.EggCleanerService;
import com.lunatech.dragonegghunt.utility.EggItemFactory;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Passive listener that monitors events to clean illegal (decorative/duplicate) dragon eggs
 * in real-time as players join, interact with inventories, or place/break blocks.
 */
public final class EggCleanerListener implements Listener {

    private final DragonEggHunt plugin;
    private final EggCleanerService eggCleanerService;

    public EggCleanerListener(DragonEggHunt plugin) {
        this.plugin = plugin;
        this.eggCleanerService = plugin.getEggCleanerService();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        int count = eggCleanerService.cleanPlayer(player);
        if (count > 0) {
            plugin.getSLF4JLogger().warn("Scrubbed {} illegal dragon egg(s) from player {} upon join.", count, player.getName());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        final org.bukkit.inventory.Inventory inventory = event.getInventory();
        if (inventory.getHolder() instanceof dev.triumphteam.gui.guis.BaseGui) {
            return;
        }
        final int count = eggCleanerService.cleanInventory(inventory);
        if (count > 0) {
            plugin.getSLF4JLogger().warn("Scrubbed {} illegal dragon egg(s) from opened container inventory (viewer: {}).", 
                count, event.getPlayer().getName());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        final org.bukkit.inventory.Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory != null && clickedInventory.getHolder() instanceof dev.triumphteam.gui.guis.BaseGui) {
            return;
        }

        final ItemStack clicked = event.getCurrentItem();
        final ItemStack cursor = event.getCursor();
        int count = 0;

        if (clicked != null && clicked.getType() == Material.DRAGON_EGG && !EggItemFactory.isAlphaEgg(clicked)) {
            event.setCurrentItem(null);
            count++;
        }
        if (cursor != null && cursor.getType() == Material.DRAGON_EGG && !EggItemFactory.isAlphaEgg(cursor)) {
            event.getWhoClicked().setItemOnCursor(null);
            count++;
        }

        if (count > 0) {
            plugin.getSLF4JLogger().warn("Scrubbed {} illegal dragon egg(s) from player {}'s inventory click interaction.", 
                count, event.getWhoClicked().getName());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack itemPlaced = event.getItemInHand();
        if (itemPlaced.getType() == Material.DRAGON_EGG && !EggItemFactory.isAlphaEgg(itemPlaced)) {
            event.setCancelled(true);
            event.getPlayer().getInventory().removeItem(itemPlaced);
            plugin.getSLF4JLogger().warn("Prevented player {} from placing an illegal dragon egg block and removed it.", 
                event.getPlayer().getName());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.DRAGON_EGG) {
            return;
        }

        // Attempt to clean the placed block if it is illegal
        if (eggCleanerService.cleanPlacedBlock(block)) {
            event.setDropItems(false);
            event.setCancelled(true);
            plugin.getSLF4JLogger().warn("Cleaned illegal decorative dragon egg block at world: {}, x: {}, y: {}, z: {}", 
                block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        }
    }
}
