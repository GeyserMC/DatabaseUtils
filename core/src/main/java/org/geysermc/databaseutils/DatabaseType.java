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
    H2(DatabaseCategory.SQL, SqlDialect.H2, "H2"),
    SQL_SERVER(DatabaseCategory.SQL, SqlDialect.SQL_SERVER, "SQL Server"),
    MYSQL(DatabaseCategory.SQL, SqlDialect.MYSQL, "MySQL"),
    MARIADB(DatabaseCategory.SQL, SqlDialect.MARIADB, "MariaDB"),
    ORACLE_DATABASE(DatabaseCategory.SQL, SqlDialect.ORACLE_DATABASE, "Oracle Database"),
    POSTGRESQL(DatabaseCategory.SQL, SqlDialect.POSTGRESQL, "PostgreSQL"),
    SQLITE(DatabaseCategory.SQL, SqlDialect.SQLITE, "SQLite"),
    MONGODB(DatabaseCategory.MONGODB, null, "MongoDB");

    public static final DatabaseType[] VALUES = values();

    private final DatabaseCategory databaseCategory;
    private final SqlDialect dialect;
    private final String friendlyName;

    DatabaseType(@NonNull DatabaseCategory databaseCategory, @Nullable SqlDialect dialect, String friendlyName) {
        this.databaseCategory = Objects.requireNonNull(databaseCategory);
        this.dialect = dialect;
        this.friendlyName = friendlyName;
    }

    public @NonNull DatabaseCategory databaseCategory() {
        return databaseCategory;
    }

    public @Nullable SqlDialect dialect() {
        return dialect;
    }

    public @NonNull String friendlyName() {
        return friendlyName;
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

    @Override
    public String toString() {
        return friendlyName();
    }
}
