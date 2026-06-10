package com.lunatech.dragonegghunt.hook.axgraves;

import com.lunatech.dragonegghunt.AbstractPlugin;
import com.lunatech.dragonegghunt.DragonEggHunt;
import com.lunatech.dragonegghunt.hook.AbstractHook;
import com.lunatech.dragonegghunt.utility.EggItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;
import java.lang.reflect.Method;

/**
 * Dynamic, reflection-based hook for the AxGraves plugin.
 * Extends AbstractHook and listens to GraveSpawnEvent to safely scrub
 * duplicate Alpha Dragon Eggs from graves on player death.
 */
public final class AxGravesHook extends AbstractHook {

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
        if (!isPluginEnabled("AxGraves")) {
            return;
        }

        try {
            // Reflectively load AxGraves' GraveSpawnEvent and Grave classes
            @SuppressWarnings("unchecked")
            Class<? extends Event> spawnEventClass = (Class<? extends Event>) Class.forName(
                "com.artillexstudios.axgraves.api.events.GraveSpawnEvent"
            );
            Class<?> graveClass = Class.forName("com.artillexstudios.axgraves.grave.Grave");

            // Fetch methods to get the Grave instance from the event and the Inventory GUI from the Grave
            Method getGraveMethod = spawnEventClass.getMethod("getGrave");
            Method getGuiMethod = graveClass.getMethod("getGui");

            // Construct an EventExecutor to handle the GraveSpawnEvent dynamically
            EventExecutor executor = (listener, event) -> {
                if (!spawnEventClass.isInstance(event)) {
                    return;
                }
                try {
                    // grave = event.getGrave()
                    Object grave = getGraveMethod.invoke(event);
                    if (grave == null) {
                        return;
                    }
                    // gui = grave.getGui()
                    Inventory gui = (Inventory) getGuiMethod.invoke(grave);
                    if (gui == null) {
                        return;
                    }

                    // Scan the grave's inventory and remove any instance of the Alpha Dragon Egg
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
                } catch (Throwable t) {
                    getPlugin().getSLF4JLogger().error("Failed to process GraveSpawnEvent reflectively in AxGravesHook", t);
                }
            };

            // Register the event dynamically
            Bukkit.getPluginManager().registerEvent(
                spawnEventClass,
                new Listener() {},
                EventPriority.NORMAL,
                executor,
                getPlugin()
            );

            loaded = true;
            getPlugin().getSLF4JLogger().info("Successfully registered dynamic AxGraves integration hook.");

        } catch (ClassNotFoundException e) {
            getPlugin().getSLF4JLogger().warn("AxGraves classes not found despite the plugin being enabled.");
        } catch (NoSuchMethodException e) {
            getPlugin().getSLF4JLogger().error("Incompatible AxGraves API version detected (missing expected methods).", e);
        } catch (Throwable t) {
            getPlugin().getSLF4JLogger().error("Unexpected error initializing AxGraves hook.", t);
        }
    }
}
