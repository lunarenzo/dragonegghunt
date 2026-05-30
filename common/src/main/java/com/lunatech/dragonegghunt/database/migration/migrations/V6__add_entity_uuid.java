package com.lunatech.dragonegghunt.database.migration.migrations;

import com.lunatech.dragonegghunt.database.migration.MigrationUtils;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jooq.DSLContext;

import java.sql.Connection;

/**
 * Migration to add the entity_uuid column to the dragon_egg_state table.
 */
public class V6__add_entity_uuid extends BaseJavaMigration {
    @Override
    public void migrate(final Context flywayContext) throws Exception {
        final Connection connection = flywayContext.getConnection();
        final DSLContext context = MigrationUtils.getContext(connection);

        context.execute("ALTER TABLE " + context.render(org.jooq.impl.DSL.table(org.jooq.impl.DSL.name("dragon_egg_state"))) + " ADD COLUMN entity_uuid VARCHAR(36)");
    }
}
