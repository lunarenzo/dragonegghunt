package com.lunatech.dragonegghunt.persistence;

import java.util.List;

/**
 * Interface for writing and cleaning transition logs in the database.
 */
public interface AuditLogWriter {
    /**
     * Saves a single transition log.
     *
     * @param log the log to save
     */
    void save(TransitionLog log);

    /**
     * Saves a list of transition logs in a single batch insert.
     *
     * @param logs the list of logs to save
     */
    void saveBatch(List<TransitionLog> logs);

    /**
     * Deletes log entries that were created before the specified timestamp.
     *
     * @param epochMillis the cut-off epoch timestamp in milliseconds
     */
    void pruneLogsOlderThan(long epochMillis);
}
