/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.type.mongo;

import java.util.HashMap;
import java.util.Map;
import javax.lang.model.element.Name;
import org.geysermc.databaseutils.processor.util.StringUtils;

public class MongoTypeMappingRegistry {
    private static final Map<String, String> MAPPINGS = new HashMap<>();

    public static String mongoTypeFor(Name typeName) {
        return MAPPINGS.get(typeName.toString());
    }

    private static void addMapping(Class<?> type) {
        addMapping(type, type.getSimpleName());
    }

    private static void addMapping(Class<?> type, String mapping) {
        MAPPINGS.put(type.getCanonicalName(), StringUtils.capitalize(mapping));
    }

    static {
        addMapping(Boolean.class);
        addMapping(Integer.class, "int32");
        addMapping(Long.class, "int64");
        addMapping(Double.class);
        addMapping(String.class);
    }
}
