package com.lunatech.dragonegghunt.persistence.impl;

import com.lunatech.dragonegghunt.persistence.TransitionLog;
import com.lunatech.dragonegghunt.persistence.AuditLogRepository;
import com.lunatech.dragonegghunt.utility.DB;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

/**
 * SQL-backed implementation of AuditLogRepository.
 */
public class SqlAuditLogRepository implements AuditLogRepository {

    @Override
    public List<TransitionLog> getRecentLogs(int limit) {
        List<TransitionLog> logs = new ArrayList<>();
        try (Connection con = DB.getConnection()) {
            DSLContext context = DB.getContext(con);
            Result<Record> result = context.select()
                .from(table(name("egg_audit_logs")))
                .orderBy(field(name("logged_at")).desc())
                .limit(limit)
                .fetch();

            for (Record record : result) {
                Integer id = record.get(field(name("id")), Integer.class);
                String actionType = record.get(field(name("action_type")), String.class);
                String playerUuidStr = record.get(field(name("player_uuid")), String.class);
                String worldName = record.get(field(name("world_name")), String.class);
                Double x = record.get(field(name("x")), Double.class);
                Double y = record.get(field(name("y")), Double.class);
                Double z = record.get(field(name("z")), Double.class);
                Long loggedAt = record.get(field(name("logged_at")), Long.class);

                UUID playerUuid = null;
                if (playerUuidStr != null && !playerUuidStr.isBlank()) {
                    try {
                        playerUuid = UUID.fromString(playerUuidStr);
                    } catch (IllegalArgumentException ignored) {}
                }

                logs.add(new TransitionLog(
                    id,
                    actionType != null ? actionType : "UNKNOWN",
                    playerUuid,
                    worldName != null ? worldName : "unknown",
                    x != null ? x : 0.0,
                    y != null ? y : 0.0,
                    z != null ? z : 0.0,
                    loggedAt != null ? loggedAt : 0L
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    @Override
    public void save(@NotNull TransitionLog log) {
        try (Connection con = DB.getConnection()) {
            DSLContext context = DB.getContext(con);
            context.insertInto(table(name("egg_audit_logs")))
                .set(field(name("action_type")), log.actionType())
                .set(field(name("player_uuid")), log.playerUuid() != null ? log.playerUuid().toString() : null)
                .set(field(name("world_name")), log.worldName())
                .set(field(name("x")), log.x())
                .set(field(name("y")), log.y())
                .set(field(name("z")), log.z())
                .set(field(name("logged_at")), log.loggedAt())
                .execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveBatch(List<TransitionLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        try (Connection con = DB.getConnection()) {
            DSLContext context = DB.getContext(con);
            var query = context.insertInto(table(name("egg_audit_logs")),
                field(name("action_type")),
                field(name("player_uuid")),
                field(name("world_name")),
                field(name("x")),
                field(name("y")),
                field(name("z")),
                field(name("logged_at"))
            );
            for (TransitionLog log : logs) {
                query = query.values(
                    log.actionType(),
                    log.playerUuid() != null ? log.playerUuid().toString() : null,
                    log.worldName(),
                    log.x(),
                    log.y(),
                    log.z(),
                    log.loggedAt()
                );
            }
            query.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void pruneLogsOlderThan(long epochMillis) {
        try (Connection con = DB.getConnection()) {
            DSLContext context = DB.getContext(con);
            context.deleteFrom(table(name("egg_audit_logs")))
                .where(field(name("logged_at")).lt(epochMillis))
                .execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
