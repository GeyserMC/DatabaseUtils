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
import org.geysermc.databaseutils.processor.query.section.projection.keyword.SkipProjectionKeyword;
import org.geysermc.databaseutils.processor.query.section.projection.keyword.TopProjectionKeyword;

public class ProjectionKeywordRegistry {
    private static final Map<Pattern, Supplier<ProjectionKeyword>> REGISTRY = new HashMap<>();

    public static @Nullable ProjectionKeyword findByName(String keyword) {
        for (var entry : REGISTRY.entrySet()) {
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
        REGISTRY.put(Pattern.compile(instance.name()), keywordSupplier);
    }

    static {
        register(DistinctProjectionKeyword::new);
        register(AvgProjectionKeyword::new);
        register(TopProjectionKeyword::new);
        register(SkipProjectionKeyword::new);
    }
}
