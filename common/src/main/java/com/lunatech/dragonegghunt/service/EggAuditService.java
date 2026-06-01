package com.lunatech.dragonegghunt.service;

import com.lunatech.dragonegghunt.persistence.TransitionLog;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service to manage Egg Lifecycle Transition logs and administrative metrics.
 */
public interface EggAuditService {

    /**
     * Initializes the service by loading cached entries and scheduling background tasks.
     * Must be called during plugin enablement after the database is started.
     */
    void start();

    /**
     * Enqueues a log transition event to be written asynchronously.
     * Execution takes under 1 microsecond and is completely off-thread friendly.
     */
    void logTransition(String actionType, java.util.UUID playerUuid, String worldName, double x, double y, double z);

    /**
     * Gets the latest transition logs from the local in-memory RAM cache instantly.
     *
     * @return a list of the 10 most recent transition logs
     */
    List<TransitionLog> getCachedLogs();

    /**
     * Fetches recent logs from the database asynchronously.
     *
     * @param limit maximum number of logs to return
     * @return a CompletableFuture containing the recent logs
     */
    CompletableFuture<List<TransitionLog>> getRecentLogsAsync(int limit);

    /**
     * Triggers asynchronous pruning of logs older than 30 days.
     */
    void pruneLogsAsync();

    /**
     * Forces writing all queued events to database synchronously (used during plugin disable).
     */
    void flushQueueSync();
}
