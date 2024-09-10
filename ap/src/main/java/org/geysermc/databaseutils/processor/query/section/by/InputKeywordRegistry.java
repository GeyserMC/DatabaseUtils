/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.query.section.by;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.databaseutils.processor.query.section.by.keyword.EqualsKeyword;
import org.geysermc.databaseutils.processor.query.section.by.keyword.LessThanKeyword;
import org.geysermc.databaseutils.processor.query.section.by.keyword.NotNullKeyword;
import org.geysermc.databaseutils.processor.query.section.by.keyword.NullKeyword;

public class InputKeywordRegistry {
    private static final Map<String, InputKeyword> STATIC_REGISTRY = new HashMap<>();
    private static final Map<String, Supplier<InputKeyword>> DYNAMIC_REGISTRY = new HashMap<>();

    public static @Nullable InputKeyword findByName(String keyword) {
        var instance = STATIC_REGISTRY.get(keyword);
        if (instance != null) {
            return instance;
        }
        var supplier = DYNAMIC_REGISTRY.get(keyword);
        return supplier != null ? supplier.get() : null;
    }

    private static void register(Supplier<InputKeyword> keywordSupplier) {
        var instance = keywordSupplier.get();
        for (@NonNull String name : instance.names()) {
            DYNAMIC_REGISTRY.put(name, keywordSupplier);
        }
    }

    private static void register(InputKeyword instance) {
        for (@NonNull String name : instance.names()) {
            STATIC_REGISTRY.put(name, instance);
        }
    }

    static {
        register(EqualsKeyword::new);
        register(NullKeyword.INSTANCE);
        register(NotNullKeyword.INSTANCE);

        register(LessThanKeyword::new);
    }
}
