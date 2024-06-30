/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.util;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;

public final class ClassUtils {
    private ClassUtils() {}

    public static boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static <T extends AccessibleObject> T access(T member) {
        member.setAccessible(true);
        return member;
    }

    public static <T> T staticCastedValue(Field field) {
        try {
            //noinspection unchecked
            return (T) access(field).get(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T staticCastedValue(Field field, Class<T> type) {
        return staticCastedValue(field);
    }
}
