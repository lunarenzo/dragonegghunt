package com.lunatech.dragonegghunt.service.impl;

import com.lunatech.dragonegghunt.DragonEggHunt;
import com.lunatech.dragonegghunt.service.EggCleanerService;
import com.lunatech.dragonegghunt.state.EggState;
import com.lunatech.dragonegghunt.utility.EggItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of EggCleanerService.
 * Designed with modern Java 21/25 features and extreme performance optimizations
 * (such as ChunkSnapshot block section checks to avoid Bukkit Block object allocations).
 */
public final class EggCleanerServiceImpl implements EggCleanerService {

    private final DragonEggHunt plugin;

    public EggCleanerServiceImpl(DragonEggHunt plugin) {
        this.plugin = plugin;
    }

    @Override
    public int cleanInventory(@NotNull Inventory inventory) {
        ItemStack[] contents = inventory.getContents();
        int count = 0;
        boolean modified = false;
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null) {
                int cleaned = cleanItem(item);
                if (cleaned > 0) {
                    count += cleaned;
                    modified = true;
                    if (item.getAmount() <= 0) {
                        contents[i] = null;
                    }
                }
            }
        }
        if (modified) {
            inventory.setContents(contents);
        }
        return count;
    }

    private int cleanItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0;
        }

        int count = 0;

        // If it is an illegal dragon egg itself
        if (item.getType() == Material.DRAGON_EGG && !EggItemFactory.isAlphaEgg(item)) {
            item.setAmount(0); // Mark for removal
            return 1;
        }

        // If it is a Shulker Box item
        if (item.hasItemMeta() && item.getItemMeta() instanceof org.bukkit.inventory.meta.BlockStateMeta bmeta) {
            if (bmeta.getBlockState() instanceof org.bukkit.block.ShulkerBox shulker) {
                Inventory shulkerInv = shulker.getInventory();
                ItemStack[] contents = shulkerInv.getContents();
                boolean modified = false;
                for (int i = 0; i < contents.length; i++) {
                    ItemStack innerItem = contents[i];
                    int cleaned = cleanItem(innerItem);
                    if (cleaned > 0) {
                        count += cleaned;
                        modified = true;
                        if (innerItem == null || innerItem.getAmount() <= 0) {
                            contents[i] = null;
                        }
                    }
                }
                if (modified) {
                    shulkerInv.setContents(contents);
                    bmeta.setBlockState(shulker);
                    item.setItemMeta(bmeta);
                }
            }
        }

        // If it is a Bundle item
        if (item.hasItemMeta() && item.getItemMeta() instanceof org.bukkit.inventory.meta.BundleMeta bmeta) {
            List<ItemStack> bundleItems = new ArrayList<>(bmeta.getItems());
            boolean bundleModified = false;
            for (int i = 0; i < bundleItems.size(); i++) {
                ItemStack innerItem = bundleItems.get(i);
                int cleaned = cleanItem(innerItem);
                if (cleaned > 0) {
                    count += cleaned;
                    bundleModified = true;
                    if (innerItem == null || innerItem.getAmount() <= 0) {
                        bundleItems.remove(i);
                        i--;
                    }
                }
            }
            if (bundleModified) {
                bmeta.setItems(bundleItems);
                item.setItemMeta(bmeta);
            }
        }

        return count;
    }

    @Override
    public int cleanPlayer(@NotNull Player player) {
        int count = cleanInventory(player.getInventory());
        count += cleanInventory(player.getEnderChest());
        
        // Clean items on cursor
        org.bukkit.inventory.InventoryView openInv = player.getOpenInventory();
        if (openInv != null) {
            ItemStack cursor = openInv.getCursor();
            if (cursor != null && cursor.getType() == Material.DRAGON_EGG && !EggItemFactory.isAlphaEgg(cursor)) {
                openInv.setCursor(null);
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean cleanPlacedBlock(@NotNull Block block) {
        if (block.getType() != Material.DRAGON_EGG) {
            return false;
        }

        EggState state = plugin.getEggTrackerService().getState();
        boolean isReal = false;
        if (state instanceof EggState.Placed placed) {
            isReal = block.getWorld().getName().equals(placed.worldName())
                && block.getX() == (int) Math.floor(placed.x())
                && block.getY() == (int) Math.floor(placed.y())
                && block.getZ() == (int) Math.floor(placed.z());
        }

        if (!isReal) {
            block.setType(Material.AIR);
            return true;
        }
        return false;
    }

    private record ChunkTask(
        ChunkSnapshot snapshot,
        int minSection,
        int maxSection
    ) {}

    private record IllegalBlockLocation(
        String worldName,
        int x,
        int y,
        int z
    ) {}

    @Override
    public @NotNull CompletableFuture<CleanupReport> runActivePurge() {
        long startTime = System.currentTimeMillis();
        int onlinePlayersScanned = 0;
        int eggsRemovedFromInventories = 0;
        int eggsRemovedFromEnderChests = 0;

        // 1. Scan online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            onlinePlayersScanned++;
            eggsRemovedFromInventories += cleanInventory(player.getInventory());
            eggsRemovedFromEnderChests += cleanInventory(player.getEnderChest());
            
            // Cursor item check
            org.bukkit.inventory.InventoryView openInv = player.getOpenInventory();
            if (openInv != null) {
                ItemStack cursor = openInv.getCursor();
                if (cursor != null && cursor.getType() == Material.DRAGON_EGG && !EggItemFactory.isAlphaEgg(cursor)) {
                    openInv.setCursor(null);
                    eggsRemovedFromInventories++;
                }
            }
        }

        // 2. Scan loaded chunks for illegal placed blocks (capture snapshots synchronously)
        List<ChunkTask> chunkTasks = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            int minHeight = world.getMinHeight();
            int maxHeight = world.getMaxHeight();
            int minSection = minHeight >> 4;
            int maxSection = (maxHeight - 1) >> 4;
            
            for (Chunk chunk : world.getLoadedChunks()) {
                ChunkSnapshot snapshot = chunk.getChunkSnapshot(false, false, false);
                chunkTasks.add(new ChunkTask(snapshot, minSection, maxSection));
            }
        }

        int loadedChunksScanned = chunkTasks.size();
        EggState state = plugin.getEggTrackerService().getState();

        final int finalOnlinePlayersScanned = onlinePlayersScanned;
        final int finalEggsRemovedFromInventories = eggsRemovedFromInventories;
        final int finalEggsRemovedFromEnderChests = eggsRemovedFromEnderChests;
        final int finalLoadedChunksScanned = loadedChunksScanned;

        // 3. Scan the snapshots asynchronously
        CompletableFuture<List<IllegalBlockLocation>> asyncScanFuture = CompletableFuture.supplyAsync(() -> {
            List<IllegalBlockLocation> locationsToClear = new ArrayList<>();
            for (ChunkTask task : chunkTasks) {
                ChunkSnapshot snapshot = task.snapshot();
                int minSec = task.minSection();
                int maxSec = task.maxSection();
                String worldName = snapshot.getWorldName();
                int chunkX = snapshot.getX();
                int chunkZ = snapshot.getZ();

                for (int sectionY = minSec; sectionY <= maxSec; sectionY++) {
                    if (snapshot.isSectionEmpty(sectionY - minSec)) {
                        continue; // Skip empty sections
                    }
                    
                    int startY = sectionY << 4;
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            for (int y = 0; y < 16; y++) {
                                int absoluteY = startY + y;
                                if (snapshot.getBlockType(x, absoluteY, z) == Material.DRAGON_EGG) {
                                    // Check if this matches the real placed egg location
                                    boolean isReal = false;
                                    if (state instanceof EggState.Placed placed) {
                                        isReal = worldName.equals(placed.worldName())
                                            && (chunkX * 16 + x) == (int) Math.floor(placed.x())
                                            && absoluteY == (int) Math.floor(placed.y())
                                            && (chunkZ * 16 + z) == (int) Math.floor(placed.z());
                                    }
                                    
                                    if (!isReal) {
                                        locationsToClear.add(new IllegalBlockLocation(
                                            worldName,
                                            chunkX * 16 + x,
                                            absoluteY,
                                            chunkZ * 16 + z
                                        ));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return locationsToClear;
        });

        // 4. Return to main thread to safely set blocks to AIR
        CompletableFuture<CleanupReport> resultFuture = new CompletableFuture<>();
        asyncScanFuture.thenAccept(locationsToClear -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                int blocksRemovedFromWorlds = 0;
                for (IllegalBlockLocation loc : locationsToClear) {
                    World world = Bukkit.getWorld(loc.worldName());
                    if (world == null) {
                        continue;
                    }
                    int chunkX = loc.x() >> 4;
                    int chunkZ = loc.z() >> 4;
                    if (!world.isChunkLoaded(chunkX, chunkZ)) {
                        continue; // Avoid loading chunks asynchronously
                    }
                    Block block = world.getBlockAt(loc.x(), loc.y(), loc.z());
                    if (block.getType() == Material.DRAGON_EGG) {
                        block.setType(Material.AIR);
                        blocksRemovedFromWorlds++;
                    }
                }

                long elapsedTime = System.currentTimeMillis() - startTime;
                resultFuture.complete(new CleanupReport(
                    finalOnlinePlayersScanned,
                    finalLoadedChunksScanned,
                    finalEggsRemovedFromInventories,
                    finalEggsRemovedFromEnderChests,
                    blocksRemovedFromWorlds,
                    elapsedTime
                ));
            });
        }).exceptionally(ex -> {
            resultFuture.completeExceptionally(ex);
            return null;
        });

        return resultFuture;
    }
}
