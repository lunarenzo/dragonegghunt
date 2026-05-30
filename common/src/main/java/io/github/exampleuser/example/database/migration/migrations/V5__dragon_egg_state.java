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

        context.createTableIfNotExists(name("dragon_egg_state"))
            .column(field("id", INTEGER.notNull()))
            .column(field("state_type", VARCHAR(50).notNull()))
            .column(field("holder_uuid", VARCHAR(36)))
            .column(field("world_name", VARCHAR(255)))
            .column(field("x", DOUBLE))
            .column(field("y", DOUBLE))
            .column(field("z", DOUBLE))
            .column(field("since", BIGINT))
            .constraints(org.jooq.impl.DSL.constraint(name("pk_dragon_egg_state")).primaryKey(name("id")))
            .execute();
    }
}
