package com.lunatech.dragonegghunt.service.impl;

import com.lunatech.dragonegghunt.persistence.AuditLogRepository;
import com.lunatech.dragonegghunt.persistence.TransitionLog;
import com.lunatech.dragonegghunt.service.EggAuditService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Default implementation of EggAuditService.
 * Uses a thread-safe local queue and RAM cache to keep database writes completely asynchronous.
 */
public class DefaultEggAuditService implements EggAuditService {

    private final AuditLogRepository repository;
    private final ConcurrentLinkedQueue<TransitionLog> queue = new ConcurrentLinkedQueue<>();
    private final List<TransitionLog> cache = new CopyOnWriteArrayList<>();
    private ScheduledExecutorService scheduler;

    public DefaultEggAuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void start() {
        if (scheduler != null) {
            return;
        }

        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "dragonegghunt-audit-worker");
            thread.setDaemon(true);
            return thread;
        });

        // Initial load of latest 10 items into RAM cache asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                List<TransitionLog> recent = repository.getRecentLogs(10);
                cache.addAll(recent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Run batch database write every 10 seconds
        scheduler.scheduleAtFixedRate(this::flushQueue, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    public void logTransition(String actionType, UUID playerUuid, String worldName, double x, double y, double z) {
        TransitionLog log = new TransitionLog(
            null,
            actionType,
            playerUuid,
            worldName,
            x,
            y,
            z,
            System.currentTimeMillis()
        );

        // Limit queue size to prevent Out of Memory issues if database fails
        if (queue.size() < 1000) {
            queue.add(log);
        }

        // Add to the front of the RAM cache
        cache.add(0, log);
        while (cache.size() > 10) {
            cache.remove(cache.size() - 1);
        }
    }

    @Override
    public List<TransitionLog> getCachedLogs() {
        return Collections.unmodifiableList(cache);
    }

    @Override
    public CompletableFuture<List<TransitionLog>> getRecentLogsAsync(int limit) {
        return CompletableFuture.supplyAsync(() -> repository.getRecentLogs(limit));
    }

    @Override
    public void pruneLogsAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                long thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);
                repository.pruneLogsOlderThan(thirtyDaysAgo);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void flushQueueSync() {
        if (scheduler != null) {
            // Shutdown the background scheduler
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            scheduler = null;
        }

        // Final synchronous flush of remaining queue elements
        flushQueue();
    }

    private void flushQueue() {
        try {
            List<TransitionLog> toWrite = new ArrayList<>();
            TransitionLog log;
            while ((log = queue.poll()) != null) {
                toWrite.add(log);
            }

            if (!toWrite.isEmpty()) {
                repository.saveBatch(toWrite);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
