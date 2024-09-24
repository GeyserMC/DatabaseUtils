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

    public static String sqlTypeFor(Class<?> type, SqlDialect dialect, int maxLength) {
        var dialectMapping = DIALECT_MAPPINGS.getOrDefault(dialect, Collections.emptyMap());
        var mapping = dialectMapping.getOrDefault(type, dialectMapping.get(Byte[].class));
        if (mapping != null) {
            return mapping.formatted(maxLength);
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
        // see the README for more info, data is based off LimitsEnforcer
        addMapping(Boolean.class, "boolean");
        addDialectMapping(SqlDialect.SQL_SERVER, Boolean.class, "bit");
        addDialectMapping(SqlDialect.SQLITE, Boolean.class, "int");

        addMapping(Byte.class, "tinyint");
        addDialectMapping(SqlDialect.POSTGRESQL, Byte.class, "smallint");
        addDialectMapping(SqlDialect.SQL_SERVER, Byte.class, "smallint");

        addMapping(Short.class, "smallint");

        addMapping(Character.class, "int");
        addDialectMapping(SqlDialect.MARIADB, Short.class, "smallint unsigned");
        addDialectMapping(SqlDialect.MYSQL, Short.class, "smallint unsigned");
        addDialectMapping(SqlDialect.ORACLE_DATABASE, Character.class, "number(5)");

        addMapping(Integer.class, "int");
        addMapping(Long.class, "bigint");

        addMapping(Float.class, "real");
        addDialectMapping(SqlDialect.ORACLE_DATABASE, Float.class, "binary_float");

        addMapping(Double.class, "double precision");
        addDialectMapping(SqlDialect.ORACLE_DATABASE, Double.class, "binary_double");
        addDialectMapping(SqlDialect.SQL_SERVER, Double.class, "float");

        addMapping(String.class, "varchar(%s)");
        addDialectMapping(SqlDialect.ORACLE_DATABASE, String.class, "varchar2(%s)");

        addMapping(Byte[].class, "varbinary(%s)");
        addDialectMapping(SqlDialect.POSTGRESQL, Byte[].class, "bytea");
        addDialectMapping(SqlDialect.ORACLE_DATABASE, Byte[].class, "raw(%s)");
    }
}
