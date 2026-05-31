package com.lunatech.dragonegghunt.service.impl;

import com.lunatech.dragonegghunt.DragonEggHunt;
import com.lunatech.dragonegghunt.config.PluginConfig;
import com.lunatech.dragonegghunt.service.TrackerRecipeService;
import io.github.milkdrinkers.colorparser.paper.ColorParser;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrackerRecipeServiceImpl implements TrackerRecipeService {

    private final DragonEggHunt plugin;
    private final NamespacedKey recipeKey;
    private final NamespacedKey trackerKey;

    public TrackerRecipeServiceImpl(DragonEggHunt plugin) {
        this.plugin = plugin;
        this.recipeKey = new NamespacedKey(plugin, "alpha_egg_tracker_recipe");
        this.trackerKey = new NamespacedKey(plugin, "alpha_egg_tracker");
    }

    @Override
    public void registerRecipe() {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::registerRecipe);
            return;
        }
        unregisterRecipe();

        PluginConfig.CompassTracker settings = plugin.getConfigHandler().getConfig().dragonEggTracker.compassTracker;
        if (!settings.enabled) {
            return;
        }

        ItemStack trackerItem = createTrackerCompass();
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, trackerItem);

        List<String> shapeList = settings.shape;
        if (shapeList == null || shapeList.isEmpty() || shapeList.size() > 3) {
            plugin.getComponentLogger().warn("Tracker recipe shape is invalid. It must contain between 1 and 3 rows.");
            return;
        }

        int maxLen = 0;
        for (String row : shapeList) {
            if (row != null && row.length() > maxLen) {
                maxLen = row.length();
            }
        }
        if (maxLen == 0 || maxLen > 3) {
            plugin.getComponentLogger().warn("Tracker recipe shape has invalid row length (must be between 1 and 3).");
            return;
        }

        List<String> rectangularShape = new ArrayList<>();
        for (String row : shapeList) {
            if (row == null) {
                row = "";
            }
            if (row.length() < maxLen) {
                row = String.format("%-" + maxLen + "s", row);
            }
            rectangularShape.add(row);
        }

        recipe.shape(rectangularShape.toArray(new String[0]));

        boolean hasIngredients = false;
        for (Map.Entry<String, String> entry : settings.ingredients.entrySet()) {
            String keyStr = entry.getKey();
            if (keyStr == null || keyStr.length() != 1) {
                plugin.getComponentLogger().warn("Tracker recipe ingredient key must be a single character: " + keyStr);
                continue;
            }
            char keyChar = keyStr.charAt(0);
            if (keyChar == ' ') {
                continue; // Reserved for air/empty
            }

            Material material = Material.matchMaterial(entry.getValue());
            if (material == null) {
                plugin.getComponentLogger().warn("Tracker recipe ingredient material not found: " + entry.getValue());
                continue;
            }

            recipe.setIngredient(keyChar, material);
            hasIngredients = true;
        }

        if (!hasIngredients) {
            plugin.getComponentLogger().warn("Tracker recipe has no valid ingredients. Registration aborted.");
            return;
        }

        try {
            Bukkit.addRecipe(recipe);
            plugin.getComponentLogger().info("Alpha Egg Tracker custom recipe registered successfully.");
        } catch (Exception e) {
            plugin.getComponentLogger().error("Failed to register custom Alpha Egg Tracker recipe: " + e.getMessage(), e);
        }
    }

    @Override
    public void unregisterRecipe() {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::unregisterRecipe);
            return;
        }
        try {
            if (Bukkit.getRecipe(recipeKey) != null) {
                Bukkit.removeRecipe(recipeKey);
            }
        } catch (Exception e) {
            plugin.getComponentLogger().error("Failed to unregister custom Alpha Egg Tracker recipe: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isTrackerCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(trackerKey, PersistentDataType.BYTE);
    }

    @Override
    public ItemStack createTrackerCompass() {
        PluginConfig.CompassTracker settings = plugin.getConfigHandler().getConfig().dragonEggTracker.compassTracker;
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(trackerKey, PersistentDataType.BYTE, (byte) 1);
            
            if (settings.displayName != null && !settings.displayName.isEmpty()) {
                meta.displayName(ColorParser.of("<!italic>" + settings.displayName).build());
            }
            
            if (settings.lore != null && !settings.lore.isEmpty()) {
                List<Component> loreComponents = new ArrayList<>();
                for (String line : settings.lore) {
                    loreComponents.add(ColorParser.of("<!italic>" + line).build());
                }
                meta.lore(loreComponents);
            }
            
            compass.setItemMeta(meta);
        }
        return compass;
    }
}
