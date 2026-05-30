package com.lunatech.dragonegghunt.listener;

import com.lunatech.dragonegghunt.DragonEggHunt;
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

    private boolean checkScheduled = false;

    private boolean isAlphaEgg(ItemStack item) {
        if (item == null || item.getType() != Material.DRAGON_EGG) {
            return false;
        }
        if (!item.hasItemMeta()) {
            return false;
        }
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(DragonEggHunt.ALPHA_EGG_KEY, org.bukkit.persistence.PersistentDataType.INTEGER);
    }

    private boolean hasAlphaEgg(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (ItemStack item : contents) {
            if (isAlphaEgg(item)) {
                return true;
            }
        }

        if (isAlphaEgg(player.getInventory().getItemInOffHand())) {
            return true;
        }

        if (isAlphaEgg(player.getItemOnCursor())) {
            return true;
        }

        org.bukkit.inventory.InventoryView openInv = player.getOpenInventory();
        if (openInv != null) {
            org.bukkit.inventory.Inventory topInventory = openInv.getTopInventory();
            if (topInventory != null) {
                org.bukkit.event.inventory.InventoryType type = topInventory.getType();
                if (type == org.bukkit.event.inventory.InventoryType.CRAFTING || 
                    type == org.bukkit.event.inventory.InventoryType.WORKBENCH) {
                    for (ItemStack item : topInventory.getContents()) {
                        if (isAlphaEgg(item)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private ItemStack stripTag(ItemStack item, Player player) {
        if (item == null) {
            return null;
        }
        ItemStack stripped = item.clone();
        org.bukkit.inventory.meta.ItemMeta meta = stripped.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().remove(DragonEggHunt.ALPHA_EGG_KEY);
            stripped.setItemMeta(meta);
        }
        player.sendMessage(plugin.getConfigHandler().getConfig().language.equals("zh_CN")
            ? "§c检测到重复的龙蛋，已将其转换为普通装饰性龙蛋！"
            : "§cDuplicate Dragon Egg detected! Converted to a normal decorative egg.");
        return stripped;
    }

    private void scanAndClean(Player player, boolean isLegitimate, boolean[] foundLegitimate) {
        ItemStack[] contents = player.getInventory().getContents();
        boolean modified = false;
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (isAlphaEgg(item)) {
                if (isLegitimate && !foundLegitimate[0]) {
                    foundLegitimate[0] = true;
                } else {
                    contents[i] = stripTag(item, player);
                    modified = true;
                }
            }
        }
        if (modified) {
            player.getInventory().setContents(contents);
        }

        ItemStack offHand = player.getInventory().getItemInOffHand();
        if (isAlphaEgg(offHand)) {
            if (isLegitimate && !foundLegitimate[0]) {
                foundLegitimate[0] = true;
            } else {
                player.getInventory().setItemInOffHand(stripTag(offHand, player));
            }
        }

        ItemStack cursor = player.getItemOnCursor();
        if (isAlphaEgg(cursor)) {
            if (isLegitimate && !foundLegitimate[0]) {
                foundLegitimate[0] = true;
            } else {
                player.setItemOnCursor(stripTag(cursor, player));
            }
        }

        org.bukkit.inventory.InventoryView openInv = player.getOpenInventory();
        if (openInv != null) {
            org.bukkit.inventory.Inventory topInventory = openInv.getTopInventory();
            if (topInventory != null) {
                org.bukkit.event.inventory.InventoryType type = topInventory.getType();
                if (type == org.bukkit.event.inventory.InventoryType.CRAFTING || 
                    type == org.bukkit.event.inventory.InventoryType.WORKBENCH) {
                    ItemStack[] topContents = topInventory.getContents();
                    boolean topModified = false;
                    for (int i = 0; i < topContents.length; i++) {
                        ItemStack item = topContents[i];
                        if (isAlphaEgg(item)) {
                            if (isLegitimate && !foundLegitimate[0]) {
                                foundLegitimate[0] = true;
                            } else {
                                topContents[i] = stripTag(item, player);
                                topModified = true;
                            }
                        }
                    }
                    if (topModified) {
                        topInventory.setContents(topContents);
                    }
                }
            }
        }
    }

    private void validateAndCleanEggs() {
        UUID legitimateHolder = null;
        UUID trackedHolder = getHolderUuid();
        if (trackedHolder != null) {
            Player trackedPlayer = Bukkit.getPlayer(trackedHolder);
            if (trackedPlayer != null && hasAlphaEgg(trackedPlayer)) {
                legitimateHolder = trackedHolder;
            }
        }

        EggState currentState = eggTrackerService.getState();
        boolean isPlaced = currentState instanceof EggState.Placed;

        if (legitimateHolder == null && !isPlaced) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (hasAlphaEgg(player)) {
                    legitimateHolder = player.getUniqueId();
                    eggTrackerService.updateState(new EggState.Held(legitimateHolder, System.currentTimeMillis()));
                    break;
                }
            }
        }

        boolean[] foundLegitimate = new boolean[]{false};
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean isLegitimate = player.getUniqueId().equals(legitimateHolder);
            scanAndClean(player, isLegitimate, foundLegitimate);
        }

        if (legitimateHolder == null && trackedHolder != null) {
            Player trackedPlayer = Bukkit.getPlayer(trackedHolder);
            if (trackedPlayer != null) {
                eggTrackerService.updateState(new EggState.Unheld());
            }
        }
    }

    private void checkPossessionDelayed() {
        if (checkScheduled) {
            return;
        }
        checkScheduled = true;
        Bukkit.getScheduler().runTask(plugin, () -> {
            checkScheduled = false;
            validateAndCleanEggs();
        });
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Item itemEntity = event.getItem();
        if (isAlphaEgg(itemEntity.getItemStack())) {
            eggTrackerService.updateState(new EggState.Held(player.getUniqueId(), System.currentTimeMillis()));
            player.sendMessage(plugin.getConfigHandler().getConfig().language.equals("zh_CN") 
                ? "§a你捡起了龙蛋！" 
                : "§aYou picked up the Dragon Egg!");
            checkPossessionDelayed();
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Item itemEntity = event.getItemDrop();
        if (isAlphaEgg(itemEntity.getItemStack())) {
            checkPossessionDelayed();
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block.getType() == Material.DRAGON_EGG && isAlphaEgg(event.getItemInHand())) {
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
            EggState state = eggTrackerService.getState();
            if (state instanceof EggState.Placed placed) {
                if (block.getWorld().getName().equals(placed.worldName()) &&
                    block.getX() == (int) Math.round(placed.x()) &&
                    block.getY() == (int) Math.round(placed.y()) &&
                    block.getZ() == (int) Math.round(placed.z())) {
                    
                    event.setDropItems(false);
                    
                    ItemStack alphaEgg = new ItemStack(Material.DRAGON_EGG);
                    org.bukkit.inventory.meta.ItemMeta meta = alphaEgg.getItemMeta();
                    if (meta != null) {
                        meta.getPersistentDataContainer().set(DragonEggHunt.ALPHA_EGG_KEY, org.bukkit.persistence.PersistentDataType.INTEGER, 1);
                        alphaEgg.setItemMeta(meta);
                    }
                    block.getWorld().dropItemNaturally(block.getLocation(), alphaEgg);
                    
                    eggTrackerService.updateState(new EggState.Unheld());
                }
            }
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
        if (event.getWhoClicked() instanceof Player) {
            checkPossessionDelayed();
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            checkPossessionDelayed();
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        checkPossessionDelayed();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID currentHolder = getHolderUuid();
        
        if (player.getUniqueId().equals(currentHolder)) {
            boolean dropped = false;
            
            ItemStack[] contents = player.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                if (isAlphaEgg(contents[i])) {
                    contents[i] = null;
                    dropped = true;
                }
            }
            player.getInventory().setContents(contents);
            
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (isAlphaEgg(offHand)) {
                player.getInventory().setItemInOffHand(null);
                dropped = true;
            }
            
            ItemStack cursor = player.getItemOnCursor();
            if (isAlphaEgg(cursor)) {
                player.setItemOnCursor(null);
                dropped = true;
            }
            
            org.bukkit.inventory.InventoryView openInv = player.getOpenInventory();
            if (openInv != null) {
                org.bukkit.inventory.Inventory topInventory = openInv.getTopInventory();
                if (topInventory != null) {
                    org.bukkit.event.inventory.InventoryType type = topInventory.getType();
                    if (type == org.bukkit.event.inventory.InventoryType.CRAFTING || 
                        type == org.bukkit.event.inventory.InventoryType.WORKBENCH) {
                        ItemStack[] topContents = topInventory.getContents();
                        for (int i = 0; i < topContents.length; i++) {
                            if (isAlphaEgg(topContents[i])) {
                                topContents[i] = null;
                                dropped = true;
                            }
                        }
                        topInventory.setContents(topContents);
                    }
                }
            }
            
            if (dropped) {
                ItemStack alphaEgg = new ItemStack(Material.DRAGON_EGG);
                org.bukkit.inventory.meta.ItemMeta meta = alphaEgg.getItemMeta();
                if (meta != null) {
                    meta.getPersistentDataContainer().set(DragonEggHunt.ALPHA_EGG_KEY, org.bukkit.persistence.PersistentDataType.INTEGER, 1);
                    alphaEgg.setItemMeta(meta);
                }
                player.getWorld().dropItemNaturally(player.getLocation(), alphaEgg);
                eggTrackerService.updateState(new EggState.Unheld());
                
                Bukkit.broadcastMessage(plugin.getConfigHandler().getConfig().language.equals("zh_CN")
                    ? "§c持有者退出了游戏，龙蛋已被丢弃在原地！"
                    : "§cThe holder has logged out. The Dragon Egg was dropped at their location!");
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryCreative(org.bukkit.event.inventory.InventoryCreativeEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            checkPossessionDelayed();
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            checkPossessionDelayed();
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        checkPossessionDelayed();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(org.bukkit.event.player.PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage().toLowerCase();
        if (msg.contains("give") || msg.contains("clear") || msg.contains("replaceitem") || msg.contains("loot") || msg.contains("item")) {
            checkPossessionDelayed();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(org.bukkit.event.server.ServerCommandEvent event) {
        String msg = event.getCommand().toLowerCase();
        if (msg.contains("give") || msg.contains("clear") || msg.contains("replaceitem") || msg.contains("loot") || msg.contains("item")) {
            checkPossessionDelayed();
        }
    }

    private UUID getHolderUuid() {
        EggState state = eggTrackerService.getState();
        if (state instanceof EggState.Held held) {
            return held.holderUuid();
        }
        return null;
    }
}
