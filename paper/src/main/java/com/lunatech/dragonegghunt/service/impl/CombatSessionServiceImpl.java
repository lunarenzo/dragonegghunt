package com.lunatech.dragonegghunt.service.impl;

import com.lunatech.dragonegghunt.DragonEggHunt;
import com.lunatech.dragonegghunt.service.CombatSessionService;
import org.jetbrains.annotations.NotNull;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.UUID;

/**
 * Implementation of CombatSessionService using ConcurrentHashMap.
 */
public final class CombatSessionServiceImpl implements CombatSessionService {

    private final DragonEggHunt plugin;
    private final ConcurrentMap<UUID, Long> activeSessions = new ConcurrentHashMap<>();

    public CombatSessionServiceImpl(@NotNull DragonEggHunt plugin) {
        this.plugin = plugin;
    }

    private long getCombatCooldownMillis() {
        return plugin.getConfigHandler().getConfig().dragonEggTracker.combatSessionDuration * 1000L;
    }

    @Override
    public void registerCombat(@NotNull UUID hunterUuid) {
        long expiry = System.currentTimeMillis() + getCombatCooldownMillis();
        activeSessions.put(hunterUuid, expiry);
    }

    @Override
    public boolean isInCombat(@NotNull UUID hunterUuid) {
        Long expiry = activeSessions.get(hunterUuid);
        if (expiry == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiry) {
            activeSessions.remove(hunterUuid); // Passive cleanup
            return false;
        }
        return true;
    }

    @Override
    public void clearSessions() {
        activeSessions.clear();
    }
}
