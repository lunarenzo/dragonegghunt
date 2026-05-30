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
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.entity.EnderDragon;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import io.github.milkdrinkers.colorparser.paper.ColorParser;
import io.github.milkdrinkers.wordweaver.Translation;

import java.util.UUID;

/**
 * Listener to track physical movement and possession transitions of the Dragon Egg.
 */
public class EggMovementListener implements Listener {

    private final DragonEggHunt plugin;
    private final EggTrackerService eggTrackerService;
    private org.bukkit.scheduler.BukkitTask validationTask;

    public EggMovementListener(DragonEggHunt plugin) {
        this.plugin = plugin;
        this.eggTrackerService = plugin.getEggTrackerService();
        this.validationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::validateAndCleanEggs, 60L, 60L);
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
        player.sendMessage(Translation.as("egghunt.duplicate-detected"));
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

    private void triggerPhoenixRespawn(String reasonKey) {
        com.lunatech.dragonegghunt.config.PluginConfig.AltarLocation altar = plugin.getConfigHandler().getConfig().dragonEggTracker.altarLocation;
        org.bukkit.World world = Bukkit.getWorld(altar.world);
        org.bukkit.Location loc;

        if (world != null) {
            loc = new org.bukkit.Location(world, altar.x, altar.y, altar.z);
        } else {
            org.bukkit.World primaryWorld = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
            if (primaryWorld != null) {
                loc = primaryWorld.getSpawnLocation();
            } else {
                return;
            }
        }

        Block block = loc.getBlock();
        block.setType(Material.DRAGON_EGG);
        eggTrackerService.updateState(new EggState.Placed(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ()));
        
        String reason = Translation.of("egghunt.reasons." + reasonKey);
        Bukkit.broadcast(
            ColorParser.of(Translation.of("egghunt.phoenix-respawn"))
                .with("reason", reason != null ? reason : reasonKey)
                .build()
        );
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
        boolean isDropped = currentState instanceof EggState.Dropped;

        if (currentState instanceof EggState.Dropped dropped) {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(dropped.entityUuid());
            boolean isLost = false;
            if (entity == null || !entity.isValid() || entity.isDead()) {
                isLost = true;
            } else if (entity.getLocation().getY() < entity.getWorld().getMinHeight() - 10) {
                isLost = true;
                entity.remove();
            }

            if (isLost) {
                org.bukkit.World world = Bukkit.getWorld(dropped.worldName());
                if (world != null) {
                    int chunkX = ((int) Math.floor(dropped.x())) >> 4;
                    int chunkZ = ((int) Math.floor(dropped.z())) >> 4;
                    if (world.isChunkLoaded(chunkX, chunkZ)) {
                        triggerPhoenixRespawn("clear");
                        return;
                    }
                }
            }
        }

        if (legitimateHolder == null && !isPlaced && !isDropped) {
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

    private boolean isTrackedAlphaEggBlock(Block block) {
        EggState state = eggTrackerService.getState();
        if (state instanceof EggState.Placed placed) {
            return block.getWorld().getName().equals(placed.worldName()) &&
                block.getX() == (int) Math.floor(placed.x()) &&
                block.getY() == (int) Math.floor(placed.y()) &&
                block.getZ() == (int) Math.floor(placed.z());
        }
        if (block.getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END) {
            if (Math.abs(block.getX()) <= 3 && Math.abs(block.getZ()) <= 3 && block.getY() >= 50 && block.getY() <= 90) {
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Item itemEntity = event.getItem();
        if (isAlphaEgg(itemEntity.getItemStack())) {
            eggTrackerService.updateState(new EggState.Held(player.getUniqueId(), System.currentTimeMillis()));
            player.sendMessage(Translation.as("egghunt.picked-up"));
            checkPossessionDelayed();
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Item itemEntity = event.getItemDrop();
        if (isAlphaEgg(itemEntity.getItemStack())) {
            itemEntity.getPersistentDataContainer().set(DragonEggHunt.ALPHA_EGG_KEY, org.bukkit.persistence.PersistentDataType.INTEGER, 1);
            
            org.bukkit.Location loc = itemEntity.getLocation();
            eggTrackerService.updateState(new EggState.Dropped(
                loc.getWorld().getName(),
                loc.getX(),
                loc.getY(),
                loc.getZ(),
                itemEntity.getUniqueId()
            ));
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
            event.getPlayer().sendMessage(Translation.as("egghunt.placed"));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDragonDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof EnderDragon) {
            org.bukkit.World world = event.getEntity().getWorld();

            // Exit portal is always at (0, 0) in the End world where the fight occurs.
            // Check every 20 ticks (1 second) for 15 seconds (15 runs)
            new org.bukkit.scheduler.BukkitRunnable() {
                private int runs = 0;

                @Override
                public void run() {
                    runs++;
                    if (runs > 15) {
                        this.cancel();
                        return;
                    }

                    // Scan a small area around (0,0) where the portal structure generates
                    for (int x = -5; x <= 5; x++) {
                        for (int z = -5; z <= 5; z++) {
                            for (int y = 50; y < 90; y++) {
                                Block b = world.getBlockAt(x, y, z);
                                if (b.getType() == Material.DRAGON_EGG) {
                                    eggTrackerService.updateState(new EggState.Placed(
                                        world.getName(),
                                        x,
                                        y,
                                        z
                                    ));
                                    plugin.getComponentLogger().info("Dragon Egg detected and tracked at exit portal area: " + x + ", " + y + ", " + z);
                                    this.cancel();
                                    return;
                                }
                            }
                        }
                    }
                }
            }.runTaskTimer(plugin, 100L, 20L); // Start checking after 5 seconds, poll every 1 second
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.DRAGON_EGG && isTrackedAlphaEggBlock(block)) {
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

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (block.getType() == Material.DRAGON_EGG && isTrackedAlphaEggBlock(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (block.getType() == Material.DRAGON_EGG && isTrackedAlphaEggBlock(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.DRAGON_EGG && isTrackedAlphaEggBlock(block)) {
            Block toBlock = event.getToBlock();
            eggTrackerService.updateState(new EggState.Placed(
                toBlock.getWorld().getName(),
                toBlock.getX(),
                toBlock.getY(),
                toBlock.getZ()
            ));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.DRAGON_EGG && isTrackedAlphaEggBlock(block)) {
            event.getEntity().getPersistentDataContainer().set(DragonEggHunt.ALPHA_EGG_KEY, org.bukkit.persistence.PersistentDataType.INTEGER, 1);
            eggTrackerService.updateState(new EggState.Unheld());
        } else if (event.getEntityType() == org.bukkit.entity.EntityType.FALLING_BLOCK && event.getTo() == Material.DRAGON_EGG) {
            if (event.getEntity().getPersistentDataContainer().has(DragonEggHunt.ALPHA_EGG_KEY, org.bukkit.persistence.PersistentDataType.INTEGER)) {
                Block targetBlock = event.getBlock();
                eggTrackerService.updateState(new EggState.Placed(
                    targetBlock.getWorld().getName(),
                    targetBlock.getX(),
                    targetBlock.getY(),
                    targetBlock.getZ()
                ));
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDropItem(EntityDropItemEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(DragonEggHunt.ALPHA_EGG_KEY, org.bukkit.persistence.PersistentDataType.INTEGER)) {
            Item itemEntity = event.getItemDrop();
            if (itemEntity.getItemStack().getType() == Material.DRAGON_EGG) {
                ItemStack item = itemEntity.getItemStack();
                org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.getPersistentDataContainer().set(DragonEggHunt.ALPHA_EGG_KEY, org.bukkit.persistence.PersistentDataType.INTEGER, 1);
                    item.setItemMeta(meta);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    private void handleExplosion(java.util.List<Block> blocks) {
        EggState state = eggTrackerService.getState();
        if (state instanceof EggState.Placed placed) {
            for (Block block : blocks) {
                if (block.getType() == Material.DRAGON_EGG &&
                    block.getWorld().getName().equals(placed.worldName()) &&
                    block.getX() == (int) Math.floor(placed.x()) &&
                    block.getY() == (int) Math.floor(placed.y()) &&
                    block.getZ() == (int) Math.floor(placed.z())) {
                    
                    block.setType(Material.AIR);
                    
                    ItemStack alphaEgg = new ItemStack(Material.DRAGON_EGG);
                    org.bukkit.inventory.meta.ItemMeta meta = alphaEgg.getItemMeta();
                    if (meta != null) {
                        meta.getPersistentDataContainer().set(DragonEggHunt.ALPHA_EGG_KEY, org.bukkit.persistence.PersistentDataType.INTEGER, 1);
                        alphaEgg.setItemMeta(meta);
                    }
                    block.getWorld().dropItemNaturally(block.getLocation(), alphaEgg);
                    
                    eggTrackerService.updateState(new EggState.Unheld());
                    break;
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
                Item itemEntity = player.getWorld().dropItemNaturally(player.getLocation(), alphaEgg);
                itemEntity.getPersistentDataContainer().set(DragonEggHunt.ALPHA_EGG_KEY, org.bukkit.persistence.PersistentDataType.INTEGER, 1);
                
                eggTrackerService.updateState(new EggState.Dropped(
                    itemEntity.getLocation().getWorld().getName(),
                    itemEntity.getLocation().getX(),
                    itemEntity.getLocation().getY(),
                    itemEntity.getLocation().getZ(),
                    itemEntity.getUniqueId()
                ));
                
                Bukkit.broadcast(Translation.as("egghunt.holder-quit-broadcast"));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Item item) {
            if (isAlphaEgg(item.getItemStack())) {
                event.setCancelled(true);
                item.remove();
                
                String reasonKey = "damage";
                switch (event.getCause()) {
                    case LAVA -> reasonKey = "lava";
                    case FIRE, FIRE_TICK -> reasonKey = "fire";
                    case CONTACT -> reasonKey = "cactus";
                    case VOID -> reasonKey = "void";
                    case ENTITY_EXPLOSION, BLOCK_EXPLOSION -> reasonKey = "explosion";
                }
                triggerPhoenixRespawn(reasonKey);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDespawn(ItemDespawnEvent event) {
        Item item = event.getEntity();
        if (isAlphaEgg(item.getItemStack())) {
            event.setCancelled(true);
            item.remove();
            triggerPhoenixRespawn("despawn");
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

        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK || event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block != null && block.getType() == Material.DRAGON_EGG && isTrackedAlphaEggBlock(block)) {
                org.bukkit.Location origin = block.getLocation();
                org.bukkit.World world = origin.getWorld();
                if (world != null) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (world.getBlockAt(origin).getType() == Material.DRAGON_EGG) {
                            return;
                        }
                        for (int dx = -8; dx <= 8; dx++) {
                            for (int dy = -8; dy <= 8; dy++) {
                                for (int dz = -8; dz <= 8; dz++) {
                                    Block target = world.getBlockAt(origin.getBlockX() + dx, origin.getBlockY() + dy, origin.getBlockZ() + dz);
                                    if (target.getType() == Material.DRAGON_EGG) {
                                        eggTrackerService.updateState(new EggState.Placed(
                                            world.getName(),
                                            target.getX(),
                                            target.getY(),
                                            target.getZ()
                                        ));
                                        plugin.getComponentLogger().info("Dragon Egg teleport tracked at: " + target.getX() + ", " + target.getY() + ", " + target.getZ());
                                        return;
                                    }
                                }
                            }
                        }
                    }, 1L);
                }
            }
        }
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

    private boolean isAllowedInventory(org.bukkit.inventory.Inventory inventory) {
        if (inventory == null) {
            return true;
        }
        org.bukkit.event.inventory.InventoryType type = inventory.getType();
        return type == org.bukkit.event.inventory.InventoryType.PLAYER ||
               type == org.bukkit.event.inventory.InventoryType.CRAFTING ||
               type == org.bukkit.event.inventory.InventoryType.WORKBENCH;
     }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClickContainer(InventoryClickEvent event) {
        if (plugin.getConfigHandler().getConfig().dragonEggTracker.allowContainerStorage) {
            return;
        }

        org.bukkit.inventory.Inventory topInventory = event.getView().getTopInventory();
        if (topInventory == null || isAllowedInventory(topInventory)) {
            return;
        }

        if (event.getClickedInventory() == topInventory) {
            if (isAlphaEgg(event.getCursor())) {
                event.setCancelled(true);
                return;
            }
            if (event.getAction() == org.bukkit.event.inventory.InventoryAction.HOTBAR_SWAP ||
                event.getAction() == org.bukkit.event.inventory.InventoryAction.HOTBAR_MOVE_AND_READD) {
                ItemStack hotbarItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
                if (isAlphaEgg(hotbarItem)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (event.getClickedInventory() == event.getView().getBottomInventory() && event.isShiftClick()) {
            if (isAlphaEgg(event.getCurrentItem())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDragContainer(InventoryDragEvent event) {
        if (plugin.getConfigHandler().getConfig().dragonEggTracker.allowContainerStorage) {
            return;
        }

        org.bukkit.inventory.Inventory topInventory = event.getView().getTopInventory();
        if (topInventory == null || isAllowedInventory(topInventory)) {
            return;
        }

        if (isAlphaEgg(event.getOldCursor())) {
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < topInventory.getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (plugin.getConfigHandler().getConfig().dragonEggTracker.allowContainerStorage) {
            return;
        }
        if (isAlphaEgg(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        if (plugin.getConfigHandler().getConfig().dragonEggTracker.allowContainerStorage) {
            return;
        }
        if (isAlphaEgg(event.getItem().getItemStack())) {
            event.setCancelled(true);
        }
    }

    private UUID getHolderUuid() {
        EggState state = eggTrackerService.getState();
        if (state instanceof EggState.Held held) {
            return held.holderUuid();
        }
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(org.bukkit.event.server.PluginDisableEvent event) {
        if (event.getPlugin().equals(plugin)) {
            if (validationTask != null) {
                validationTask.cancel();
            }
        }
    }
}
