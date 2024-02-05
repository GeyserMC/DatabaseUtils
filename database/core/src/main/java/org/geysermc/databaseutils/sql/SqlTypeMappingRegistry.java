/*
 * Copyright (c) 2024 GeyserMC <https://geysermc.org>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
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
        return DIALECT_MAPPINGS.getOrDefault(dialect, Collections.emptyMap()).get(type);
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
        addMapping(String.class, "varchar");
        addMapping(Byte[].class, "varbinary");
        addDialectMapping(SqlDialect.POSTGRESQL, Byte[].class, "bytea");
    }
}
