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
package org.geysermc.databaseutils.processor.type.sql;

import static org.geysermc.databaseutils.processor.util.StringUtils.capitalize;
import static org.geysermc.databaseutils.processor.util.StringUtils.uncapitalize;

import java.util.HashMap;
import java.util.Map;
import javax.lang.model.element.Name;

public final class JdbcTypeMappingRegistry {
    private static final Map<String, String> MAPPINGS = new HashMap<>();
    private static final Map<String, String> CONVERT_GET_FORMAT = new HashMap<>();
    private static final Map<String, String> CONVERT_SET_FORMAT = new HashMap<>();

    private JdbcTypeMappingRegistry() {}

    private static String jdbcTypeFor(Name typeName) {
        var mapping = MAPPINGS.get(typeName.toString());
        if (mapping != null) {
            return mapping;
        }
        return MAPPINGS.get(Byte[].class.getCanonicalName());
    }

    public static String jdbcGetFor(Name typeName, String format) {
        return format.formatted(
                "get" + capitalize(jdbcTypeFor(typeName)),
                CONVERT_GET_FORMAT.getOrDefault(String.valueOf(typeName), "%s").formatted("$S"));
    }

    public static String jdbcSetFor(Name typeName, String format) {
        return CONVERT_SET_FORMAT
                .getOrDefault(String.valueOf(typeName), "%s")
                .formatted(format.formatted("set" + capitalize(jdbcTypeFor(typeName))));
    }

    private static void addMapping(Class<?> type) {
        addMapping(type, uncapitalize(type.getSimpleName()));
    }

    private static void addMapping(Class<?> type, String mapping) {
        MAPPINGS.put(type.getCanonicalName(), mapping);
    }

    private static void add(Class<?> type, String mapping, String convertBothFormat) {
        addMapping(type, mapping);
        CONVERT_GET_FORMAT.put(type.getCanonicalName(), convertBothFormat);
        CONVERT_SET_FORMAT.put(type.getCanonicalName(), convertBothFormat);
    }

    private static void add(Class<?> type, String mapping, String convertGetFormat, String convertSetFormat) {
        addMapping(type, mapping);
        if (convertGetFormat != null) {
            CONVERT_GET_FORMAT.put(type.getCanonicalName(), convertGetFormat);
        }
        if (convertSetFormat != null) {
            CONVERT_SET_FORMAT.put(type.getCanonicalName(), convertSetFormat);
        }
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
