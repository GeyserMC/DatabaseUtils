package org.geysermc.databaseutils;

import java.util.Locale;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.databaseutils.sql.SqlDialect;

public enum DatabaseWithDialectType {
    H2(DatabaseType.SQL, SqlDialect.H2),
    SQL_SERVER(DatabaseType.SQL, SqlDialect.SQL_SERVER),
    MYSQL(DatabaseType.SQL, SqlDialect.MYSQL),
    ORACLE_DATABASE(DatabaseType.SQL, SqlDialect.ORACLE_DATABASE),
    POSTGRESQL(DatabaseType.SQL, SqlDialect.POSTGRESQL),
    SQLITE(DatabaseType.SQL, SqlDialect.SQLITE),
    MONGODB(DatabaseType.MONGODB, null);

    private static final DatabaseWithDialectType[] VALUES = values();

    private final DatabaseType databaseType;
    private final SqlDialect dialect;

    DatabaseWithDialectType(@NonNull DatabaseType databaseType, @Nullable SqlDialect dialect) {
        this.databaseType = Objects.requireNonNull(databaseType);
        this.dialect = dialect;
    }

    public static @Nullable DatabaseWithDialectType byName(@NonNull String name) {
        var normalized = name.replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
        for (DatabaseWithDialectType value : VALUES) {
            if (value.name().equals(normalized)) {
                return value;
            }
        }
        return null;
    }

    public @NonNull DatabaseType databaseType() {
        return databaseType;
    }

    public @Nullable SqlDialect dialect() {
        return dialect;
    }
}
