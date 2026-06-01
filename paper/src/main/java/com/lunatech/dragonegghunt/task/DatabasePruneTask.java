package com.lunatech.dragonegghunt.task;

import com.lunatech.dragonegghunt.DragonEggHunt;

/**
 * Task to periodically trigger asynchronous cleanup of database audit logs older than 30 days.
 */
public final class DatabasePruneTask implements Runnable {

    private final DragonEggHunt plugin;

    public DatabasePruneTask(DragonEggHunt plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (plugin.getEggAuditService() != null) {
            plugin.getEggAuditService().pruneLogsAsync();
        }
    }
}
