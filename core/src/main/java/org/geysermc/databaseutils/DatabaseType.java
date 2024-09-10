/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils;

import java.util.Locale;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.databaseutils.sql.SqlDialect;

public enum DatabaseType {
    H2(DatabaseCategory.SQL, SqlDialect.H2),
    SQL_SERVER(DatabaseCategory.SQL, SqlDialect.SQL_SERVER),
    MYSQL(DatabaseCategory.SQL, SqlDialect.MYSQL),
    MARIADB(DatabaseCategory.SQL, SqlDialect.MARIADB),
    ORACLE_DATABASE(DatabaseCategory.SQL, SqlDialect.ORACLE_DATABASE),
    POSTGRESQL(DatabaseCategory.SQL, SqlDialect.POSTGRESQL),
    SQLITE(DatabaseCategory.SQL, SqlDialect.SQLITE),
    MONGODB(DatabaseCategory.MONGODB, null);

    private static final DatabaseType[] VALUES = values();

    private final DatabaseCategory databaseCategory;
    private final SqlDialect dialect;

    DatabaseType(@NonNull DatabaseCategory databaseCategory, @Nullable SqlDialect dialect) {
        this.databaseCategory = Objects.requireNonNull(databaseCategory);
        this.dialect = dialect;
    }

    public static @Nullable DatabaseType byName(@NonNull String name) {
        var normalized = name.replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        for (DatabaseType value : VALUES) {
            if (value.name().equals(normalized)) {
                return value;
            }
        }
        return null;
    }

    public @NonNull DatabaseCategory databaseCategory() {
        return databaseCategory;
    }

    public @Nullable SqlDialect dialect() {
        return dialect;
    }
}
