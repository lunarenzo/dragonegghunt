package com.lunatech.dragonegghunt.listener;

import com.lunatech.dragonegghunt.Example;
import com.lunatech.dragonegghunt.service.EggTrackerService;
import com.lunatech.dragonegghunt.state.EggState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Listener to track physical movement and possession transitions of the Dragon Egg.
 */
public class EggMovementListener implements Listener {

    private final DragonEggHunt plugin;
    private final EggTrackerService eggTrackerService;

    public EggMovementListener(DragonEggHunt plugin) {
        this.plugin = plugin;
        this.eggTrackerService = plugin.getEggTrackerService();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Item itemEntity = event.getItem();
        if (itemEntity.getItemStack().getType() == Material.DRAGON_EGG) {
            eggTrackerService.updateState(new EggState.Held(player.getUniqueId(), System.currentTimeMillis()));
            player.sendMessage(plugin.getConfigHandler().getConfig().language.equals("zh_CN") 
                ? "§a你捡起了龙蛋！" 
                : "§aYou picked up the Dragon Egg!");
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Item itemEntity = event.getItemDrop();
        if (itemEntity.getItemStack().getType() == Material.DRAGON_EGG) {
            checkPossessionDelayed(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block.getType() == Material.DRAGON_EGG) {
            eggTrackerService.updateState(new EggState.Placed(
                block.getWorld().getName(),
                block.getX(),
                block.getY(),
                block.getZ()
            ));
            event.getPlayer().sendMessage(plugin.getConfigHandler().getConfig().language.equals("zh_CN")
                ? "§e你放置了龙蛋！"
                : "§eYou placed the Dragon Egg!");
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.DRAGON_EGG) {
            eggTrackerService.updateState(new EggState.Unheld());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID currentHolder = getHolderUuid();
        if (player.getUniqueId().equals(currentHolder)) {
            eggTrackerService.updateState(new EggState.Unheld());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            checkPossessionDelayed(player);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            checkPossessionDelayed(player);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        checkPossessionDelayed(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID currentHolder = getHolderUuid();
        
        if (player.getUniqueId().equals(currentHolder)) {
            boolean dropOnLogout = true; // Default behavior
            if (dropOnLogout) {
                // Remove egg from inventory
                player.getInventory().remove(Material.DRAGON_EGG);
                if (player.getItemOnCursor().getType() == Material.DRAGON_EGG) {
                    player.setItemOnCursor(null);
                }
                
                // Spawn as item at player location
                player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(Material.DRAGON_EGG));
                eggTrackerService.updateState(new EggState.Unheld());
                
                Bukkit.broadcastMessage(plugin.getConfigHandler().getConfig().language.equals("zh_CN")
                    ? "§c持有者退出了游戏，龙蛋已被丢弃在原地！"
                    : "§cThe holder has logged out. The Dragon Egg was dropped at their location!");
            }
        }
    }

    private void checkPossessionDelayed(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            boolean hasEgg = player.getInventory().contains(Material.DRAGON_EGG)
                || (player.getItemOnCursor() != null && player.getItemOnCursor().getType() == Material.DRAGON_EGG);

            UUID currentHolder = getHolderUuid();

            if (hasEgg) {
                if (!player.getUniqueId().equals(currentHolder)) {
                    eggTrackerService.updateState(new EggState.Held(player.getUniqueId(), System.currentTimeMillis()));
                }
            } else {
                if (player.getUniqueId().equals(currentHolder)) {
                    eggTrackerService.updateState(new EggState.Unheld());
                }
            }
        });
    }

    private UUID getHolderUuid() {
        EggState state = eggTrackerService.getState();
        if (state instanceof EggState.Held held) {
            return held.holderUuid();
        }
        return null;
    }
}
