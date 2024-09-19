/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.type.sql;

import static org.geysermc.databaseutils.processor.util.StringUtils.capitalize;

import java.util.HashMap;
import java.util.Map;
import javax.lang.model.element.Name;

public final class JdbcTypeMappingRegistry {
    private static final Map<String, String> MAPPINGS = new HashMap<>();
    private static final Map<String, String> CONVERT_FORMAT = new HashMap<>();

    private JdbcTypeMappingRegistry() {}

    private static String jdbcTypeFor(Name typeName) {
        var mapping = MAPPINGS.get(typeName.toString());
        if (mapping != null) {
            return mapping;
        }
        return MAPPINGS.get(Byte[].class.getCanonicalName());
    }

    public static String jdbcReadFor(Name typeName, String format) {
        return CONVERT_FORMAT
                .getOrDefault(String.valueOf(typeName), "%s")
                .formatted(format.formatted("read" + jdbcTypeFor(typeName) + "()"));
    }

    public static String jdbcGetFor(Name typeName, String format) {
        return format.formatted("get" + jdbcTypeFor(typeName) + "("
                + CONVERT_FORMAT.getOrDefault(String.valueOf(typeName), "%s").formatted("$S") + ")");
    }

    public static String jdbcSetFor(Name typeName, String format) {
        return CONVERT_FORMAT
                .getOrDefault(String.valueOf(typeName), "%s")
                .formatted(format.formatted("set" + jdbcTypeFor(typeName)));
    }

    private static void addMapping(Class<?> type) {
        addMapping(type, type.getSimpleName());
    }

    private static void addMapping(Class<?> type, String mapping) {
        MAPPINGS.put(type.getCanonicalName(), capitalize(mapping));
    }

    private static void add(Class<?> type, String mapping, String convertBothFormat) {
        addMapping(type, mapping);
        CONVERT_FORMAT.put(type.getCanonicalName(), convertBothFormat);
    }

    static {
        addMapping(Boolean.class);
        addMapping(Byte.class);
        add(Character.class, "int", "(char) %s");
        addMapping(Short.class);
        addMapping(Integer.class, "int");
        addMapping(Long.class);
        addMapping(Float.class);
        addMapping(Double.class);

        addMapping(Byte[].class, "bytes");
        addMapping(String.class);
    }
}
