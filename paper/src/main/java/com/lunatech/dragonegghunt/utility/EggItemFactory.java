package com.lunatech.dragonegghunt.utility;

import com.lunatech.dragonegghunt.DragonEggHunt;
import com.lunatech.dragonegghunt.config.PluginConfig;
import io.github.milkdrinkers.colorparser.paper.ColorParser;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class EggItemFactory {

    private EggItemFactory() {
        // Private constructor to prevent instantiation
    }

    /**
     * Creates the Alpha Dragon Egg ItemStack based on the plugin configuration.
     *
     * @param plugin the plugin instance
     * @return the custom Alpha Dragon Egg ItemStack
     */
    public static ItemStack createAlphaEgg(DragonEggHunt plugin) {
        PluginConfig.DragonEggTracker settings = plugin.getConfigHandler().getConfig().dragonEggTracker;
        ItemStack egg = new ItemStack(Material.DRAGON_EGG);
        ItemMeta meta = egg.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(DragonEggHunt.ALPHA_EGG_KEY, PersistentDataType.INTEGER, 1);

            if (settings.eggDisplayName != null && !settings.eggDisplayName.isEmpty()) {
                meta.displayName(ColorParser.of("<!italic>" + settings.eggDisplayName).build());
            }

            if (settings.eggLore != null && !settings.eggLore.isEmpty()) {
                List<Component> loreComponents = new ArrayList<>();
                for (String line : settings.eggLore) {
                    loreComponents.add(ColorParser.of("<!italic>" + line).build());
                }
                meta.lore(loreComponents);
            }

            egg.setItemMeta(meta);
        }
        return egg;
    }

    /**
     * Checks if the given ItemStack is the Alpha Dragon Egg.
     *
     * @param item the item stack to check
     * @return true if the item is the Alpha Dragon Egg, false otherwise
     */
    public static boolean isAlphaEgg(ItemStack item) {
        if (item == null || item.getType() != Material.DRAGON_EGG) {
            return false;
        }
        if (!item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(DragonEggHunt.ALPHA_EGG_KEY, PersistentDataType.INTEGER);
    }
}
