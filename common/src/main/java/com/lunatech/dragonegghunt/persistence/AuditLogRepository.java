package com.lunatech.dragonegghunt.persistence;

/**
 * Combined repository interface for the transition audit logs.
 */
public interface AuditLogRepository extends AuditLogReader, AuditLogWriter {}
