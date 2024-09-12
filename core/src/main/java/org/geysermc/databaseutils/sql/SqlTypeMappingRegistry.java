/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.sql;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class SqlTypeMappingRegistry {
    private static final Map<SqlDialect, Map<Class<?>, String>> DIALECT_MAPPINGS = new HashMap<>();

    private SqlTypeMappingRegistry() {}

    public static String sqlTypeFor(Class<?> type, SqlDialect dialect) {
        var dialectMapping = DIALECT_MAPPINGS.getOrDefault(dialect, Collections.emptyMap());
        var mapping = dialectMapping.getOrDefault(type, dialectMapping.get(Byte[].class));
        if (mapping != null) {
            return mapping;
        }
        throw new IllegalStateException(
                String.format("Was not able to find mapping for %s with dialect %s", type.getName(), dialectMapping));
    }

    private static void addDialectMapping(SqlDialect dialect, Class<?> type, String mapping) {
        DIALECT_MAPPINGS.computeIfAbsent(dialect, $ -> new HashMap<>()).put(type, mapping);
    }

    private static void addMapping(Class<?> type, String mapping) {
        for (SqlDialect dialect : SqlDialect.values()) {
            addDialectMapping(dialect, type, mapping);
        }
    }

    static {
        // see the README for more info
        addMapping(Boolean.class, "boolean");
        addDialectMapping(SqlDialect.SQL_SERVER, Boolean.class, "bit");
        addDialectMapping(SqlDialect.SQLITE, Boolean.class, "int");
        addMapping(Byte.class, "smallint");
        addMapping(Short.class, "smallint");
        addMapping(Character.class, "smallint");
        addMapping(Integer.class, "int");
        addMapping(Long.class, "bigint");
        addMapping(Float.class, "real");
        addMapping(Double.class, "double precision");
        addDialectMapping(SqlDialect.SQL_SERVER, Double.class, "float");
        // todo require all entities to specify the max length for specific types, to make sure we choose the most
        // efficient type
        // also: the max row size (for all columns combined) is 65535. Have to change some to TEXT or BLOBs
        addMapping(String.class, "varchar(1000)"); // 16383 for utf8mb4, 21844 for utf8, 65535 for one byte char set
        addMapping(Byte[].class, "varbinary(1000)"); // max = 65532, 3 byte prefix
        addDialectMapping(SqlDialect.POSTGRESQL, Byte[].class, "bytea");
        addDialectMapping(SqlDialect.ORACLE_DATABASE, Byte[].class, "raw(1000)");
    }
}
