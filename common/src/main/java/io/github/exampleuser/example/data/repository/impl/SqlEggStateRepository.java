package io.github.exampleuser.example.data.repository.impl;

import io.github.exampleuser.example.data.model.EggStateData;
import io.github.exampleuser.example.data.repository.EggStateRepository;
import io.github.exampleuser.example.utility.DB;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;

/**
 * SQL-backed implementation of EggStateRepository.
 */
public class SqlEggStateRepository implements EggStateRepository {

    @Override
    public @NotNull Optional<EggStateData> load() {
        try (Connection con = DB.getConnection()) {
            DSLContext context = DB.getContext(con);
            Record record = context.select()
                .from(name("dragon_egg_state"))
                .where(field(name("id")).eq(1))
                .fetchOne();

            if (record == null) {
                return Optional.empty();
            }

            String stateType = record.get(field(name("state_type")), String.class);
            String holderUuidStr = record.get(field(name("holder_uuid")), String.class);
            String worldName = record.get(field(name("world_name")), String.class);
            Double x = record.get(field(name("x")), Double.class);
            Double y = record.get(field(name("y")), Double.class);
            Double z = record.get(field(name("z")), Double.class);
            Long since = record.get(field(name("since")), Long.class);

            UUID holderUuid = holderUuidStr != null ? UUID.fromString(holderUuidStr) : null;

            return Optional.of(new EggStateData(stateType, holderUuid, worldName, x, y, z, since));
        } catch (SQLException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public void save(@NotNull EggStateData data) {
        try (Connection con = DB.getConnection()) {
            DSLContext context = DB.getContext(con);
            
            Integer countObj = context.selectCount()
                .from(name("dragon_egg_state"))
                .where(field(name("id")).eq(1))
                .fetchOne(0, Integer.class);
            
            boolean exists = countObj != null && countObj > 0;

            if (exists) {
                context.update(name("dragon_egg_state"))
                    .set(field(name("state_type")), data.stateType())
                    .set(field(name("holder_uuid")), data.holderUuid() != null ? data.holderUuid().toString() : null)
                    .set(field(name("world_name")), data.worldName())
                    .set(field(name("x")), data.x())
                    .set(field(name("y")), data.y())
                    .set(field(name("z")), data.z())
                    .set(field(name("since")), data.since())
                    .where(field(name("id")).eq(1))
                    .execute();
            } else {
                context.insertInto(name("dragon_egg_state"))
                    .set(field(name("id")), 1)
                    .set(field(name("state_type")), data.stateType())
                    .set(field(name("holder_uuid")), data.holderUuid() != null ? data.holderUuid().toString() : null)
                    .set(field(name("world_name")), data.worldName())
                    .set(field(name("x")), data.x())
                    .set(field(name("y")), data.y())
                    .set(field(name("z")), data.z())
                    .set(field(name("since")), data.since())
                    .execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
