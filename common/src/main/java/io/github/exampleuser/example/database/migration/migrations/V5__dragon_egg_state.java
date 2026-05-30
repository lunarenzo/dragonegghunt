package io.github.exampleuser.example.database.migration.migrations;

import io.github.exampleuser.example.database.migration.MigrationUtils;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jooq.DSLContext;

import java.sql.Connection;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.SQLDataType.*;

/**
 * Migration to create the dragon_egg_state table.
 */
public class V5__dragon_egg_state extends BaseJavaMigration {
    @Override
    public void migrate(final Context flywayContext) throws Exception {
        final Connection connection = flywayContext.getConnection();
        final DSLContext context = MigrationUtils.getContext(connection);

        context.execute("CREATE TABLE IF NOT EXISTS " + context.render(org.jooq.impl.DSL.name("dragon_egg_state")) + " (" +
            "id INTEGER NOT NULL PRIMARY KEY," +
            "state_type VARCHAR(50) NOT NULL," +
            "holder_uuid VARCHAR(36)," +
            "world_name VARCHAR(255)," +
            "x DOUBLE," +
            "y DOUBLE," +
            "z DOUBLE," +
            "since BIGINT" +
            ")");
    }
}


