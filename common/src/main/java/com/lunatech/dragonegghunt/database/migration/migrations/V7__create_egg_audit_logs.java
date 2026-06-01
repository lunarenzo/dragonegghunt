package com.lunatech.dragonegghunt.database.migration.migrations;

import com.lunatech.dragonegghunt.database.migration.MigrationUtils;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;

import java.sql.Connection;

/**
 * Migration to create the egg_audit_logs table.
 */
public class V7__create_egg_audit_logs extends BaseJavaMigration {
    @Override
    public void migrate(final Context flywayContext) throws Exception {
        final Connection connection = flywayContext.getConnection();
        final DSLContext context = MigrationUtils.getContext(connection);
        final SQLDialect dialect = context.configuration().dialect().family();

        boolean isMySQLFamily = dialect == SQLDialect.MYSQL || dialect == SQLDialect.MARIADB;
        String idColumnDefinition = isMySQLFamily 
            ? "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY" 
            : "id INTEGER NOT NULL PRIMARY KEY";

        String tableName = context.render(org.jooq.impl.DSL.table(org.jooq.impl.DSL.name("egg_audit_logs")));

        context.execute("CREATE TABLE IF NOT EXISTS " + tableName + " (" +
            idColumnDefinition + "," +
            "action_type VARCHAR(50) NOT NULL," +
            "player_uuid VARCHAR(36)," +
            "world_name VARCHAR(255) NOT NULL," +
            "x DOUBLE NOT NULL," +
            "y DOUBLE NOT NULL," +
            "z DOUBLE NOT NULL," +
            "logged_at BIGINT NOT NULL" +
            ")");

        // Create timestamp index for fast retrieval sorting
        String indexName = context.render(org.jooq.impl.DSL.name("idx_egg_audit_logs_time"));
        context.execute("CREATE INDEX IF NOT EXISTS " + indexName + " ON " + tableName + " (logged_at DESC)");
    }
}
