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
            if (item != null && item.getType() == Material.DRAGON_EGG && !EggItemFactory.isAlphaEgg(item)) {
                contents[i] = null;
                modified = true;
                count++;
            }
        }
        if (modified) {
            inventory.setContents(contents);
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

    @Override
    public @NotNull CleanupReport runActivePurge() {
        long startTime = System.currentTimeMillis();
        int onlinePlayersScanned = 0;
        int loadedChunksScanned = 0;
        int eggsRemovedFromInventories = 0;
        int eggsRemovedFromEnderChests = 0;
        int blocksRemovedFromWorlds = 0;

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

        // 2. Scan loaded chunks for illegal placed blocks
        List<Block> blocksToClear = new ArrayList<>();
        EggState state = plugin.getEggTrackerService().getState();
        
        for (World world : Bukkit.getWorlds()) {
            int minHeight = world.getMinHeight();
            int maxHeight = world.getMaxHeight();
            int minSection = minHeight >> 4;
            int maxSection = (maxHeight - 1) >> 4;
            
            for (Chunk chunk : world.getLoadedChunks()) {
                loadedChunksScanned++;
                
                // Get chunk snapshot to read block types extremely fast (avoiding Bukkit Block wrapper allocations)
                ChunkSnapshot snapshot = chunk.getChunkSnapshot(false, false, false);
                for (int sectionY = minSection; sectionY <= maxSection; sectionY++) {
                    if (snapshot.isSectionEmpty(sectionY - minSection)) {
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
                                        isReal = world.getName().equals(placed.worldName())
                                            && (chunk.getX() * 16 + x) == (int) Math.floor(placed.x())
                                            && absoluteY == (int) Math.floor(placed.y())
                                            && (chunk.getZ() * 16 + z) == (int) Math.floor(placed.z());
                                    }
                                    
                                    if (!isReal) {
                                        blocksToClear.add(chunk.getBlock(x, absoluteY, z));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Set illegal block types to AIR on the main thread
        for (Block block : blocksToClear) {
            block.setType(Material.AIR);
            blocksRemovedFromWorlds++;
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        return new CleanupReport(
            onlinePlayersScanned,
            loadedChunksScanned,
            eggsRemovedFromInventories,
            eggsRemovedFromEnderChests,
            blocksRemovedFromWorlds,
            elapsedTime
        );
    }
}
