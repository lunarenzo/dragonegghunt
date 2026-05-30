package com.lunatech.dragonegghunt.cooldown.listener;

import com.lunatech.dragonegghunt.AbstractExample;
import com.lunatech.dragonegghunt.Reloadable;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to handle registration of event listeners.
 */
@SuppressWarnings("FieldCanBeLocal")
public class ListenerHandler implements Reloadable {
    private final AbstractExample plugin;
    private final List<Listener> listeners = new ArrayList<>();

    public ListenerHandler(AbstractExample plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onLoad(AbstractExample plugin) {
    }

    @Override
    public void onEnable(AbstractExample plugin) {
        listeners.clear();
        listeners.add(new CooldownListener(plugin));

        for (Listener listener : listeners) {
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        }
    }

    @Override
    public void onDisable(AbstractExample plugin) {
    }
}
