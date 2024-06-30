/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils;

import java.util.Map;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.databaseutils.mongo.MongodbDatabase;
import org.geysermc.databaseutils.sql.SqlDatabase;

final class DatabaseRegistry {
    private static final Map<DatabaseType, Supplier<Database>> TYPES =
            Map.of(DatabaseType.SQL, SqlDatabase::new, DatabaseType.MONGODB, MongodbDatabase::new);

    public static @Nullable Database databaseFor(@NonNull DatabaseWithDialectType type) {
        var instanceSupplier = TYPES.get(type.databaseType());
        if (instanceSupplier == null) {
            return null;
        }
        return instanceSupplier.get();
    }
}
