/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.query.section.projection;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.databaseutils.processor.query.section.projection.keyword.AvgProjectionKeyword;
import org.geysermc.databaseutils.processor.query.section.projection.keyword.DistinctProjectionKeyword;
import org.geysermc.databaseutils.processor.query.section.projection.keyword.FirstProjectionKeyword;
import org.geysermc.databaseutils.processor.query.section.projection.keyword.SkipProjectionKeyword;
import org.geysermc.databaseutils.processor.query.section.projection.keyword.TopProjectionKeyword;

public class ProjectionKeywordRegistry {
    private static final Map<String, ProjectionKeyword> STATIC_REGISTRY = new HashMap<>();
    private static final Map<Pattern, Supplier<ProjectionKeyword>> DYNAMIC_REGISTRY = new HashMap<>();

    public static @Nullable ProjectionKeyword findByName(String keyword) {
        var staticInstance = STATIC_REGISTRY.get(keyword);
        if (staticInstance != null) {
            return staticInstance;
        }

        for (var entry : DYNAMIC_REGISTRY.entrySet()) {
            if (entry.getKey().matcher(keyword).matches()) {
                var instance = entry.getValue().get();
                instance.setValue(keyword);
                return instance;
            }
        }
        return null;
    }

    private static void register(Supplier<ProjectionKeyword> keywordSupplier) {
        var instance = keywordSupplier.get();
        DYNAMIC_REGISTRY.put(Pattern.compile(instance.name()), keywordSupplier);
    }

    private static void register(ProjectionKeyword keyword) {
        STATIC_REGISTRY.put(keyword.name(), keyword);
    }

    static {
        register(DistinctProjectionKeyword.INSTANCE);
        register(AvgProjectionKeyword.INSTANCE);
        register(TopProjectionKeyword::new);
        register(FirstProjectionKeyword.INSTANCE);
        register(SkipProjectionKeyword::new);
    }
}
