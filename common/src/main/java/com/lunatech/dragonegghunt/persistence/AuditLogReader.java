package com.lunatech.dragonegghunt.persistence;

import java.util.List;

/**
 * Interface for reading transition logs from the database.
 */
public interface AuditLogReader {
    /**
     * Gets a list of the most recent transition logs.
     *
     * @param limit maximum number of logs to return
     * @return a list of logs sorted by loggedAt descending
     */
    List<TransitionLog> getRecentLogs(int limit);
}
