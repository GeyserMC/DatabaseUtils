/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.util;

import java.util.HashMap;
import java.util.Map;

public final class TypedMap {
    private final Map<String, Object> items = new HashMap<>();

    public TypedMap() {}

    public void put(final String key, final Object value) {
        items.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(final String key) {
        return (T) items.get(key);
    }
}
