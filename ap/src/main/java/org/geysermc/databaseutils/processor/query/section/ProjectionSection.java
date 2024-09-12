/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.query.section;

import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.databaseutils.processor.query.section.factor.Factor;
import org.geysermc.databaseutils.processor.query.section.factor.ProjectionFactor;
import org.geysermc.databaseutils.processor.query.section.projection.ProjectionKeyword;
import org.geysermc.databaseutils.processor.query.section.projection.keyword.DistinctProjectionKeyword;
import org.geysermc.databaseutils.processor.query.section.projection.keyword.FirstProjectionKeyword;
import org.geysermc.databaseutils.processor.query.section.projection.keyword.TopProjectionKeyword;

public record ProjectionSection(List<@NonNull ProjectionFactor> projections) {
    public ProjectionSection(@NonNull List<@NonNull ProjectionFactor> projections) {
        this.projections = projections;
    }

    public static ProjectionSection from(@NonNull List<@NonNull Factor> factors) {
        if (factors.isEmpty()) {
            return null;
        }
        return new ProjectionSection(
                factors.stream().map(factor -> (ProjectionFactor) factor).toList());
    }

    public String columnName() {
        for (var factor : projections) {
            if (factor.keyword() == null) {
                return factor.columnName();
            }
        }
        return null;
    }

    public boolean distinct() {
        for (var factor : projections) {
            if (factor.keyword() instanceof DistinctProjectionKeyword) {
                return true;
            }
        }
        return false;
    }

    public boolean first() {
        for (var factor : projections) {
            if (factor.keyword() instanceof FirstProjectionKeyword) {
                return true;
            }
        }
        return false;
    }

    public int limit() {
        for (var factor : projections) {
            if (factor.keyword() instanceof FirstProjectionKeyword) {
                return 1;
            } else if (factor.keyword() instanceof TopProjectionKeyword top) {
                return top.limit();
            }
        }
        return -1;
    }

    /**
     * Returns all the projection keywords that are not:
     * <ul>
     *  <li>{@link DistinctProjectionKeyword}</li>
     *  <li>ProjectionFactor with a non-null columnName.</li>
     * </ul>
     * Those specific keywords can be gathered by their respective methods:
     * <ul>
     *   <li>{@link #distinct()}</li>
     *   <li>{@link #columnName()}</li>
     * </ul>
     */
    public @NonNull List<@NonNull ProjectionKeyword> nonSpecialProjectionKeywords() {
        return projections.stream()
                .<ProjectionKeyword>mapMulti((factor, results) -> {
                    if (factor.columnName() == null && !(factor.keyword() instanceof DistinctProjectionKeyword)) {
                        results.accept(factor.keyword());
                    }
                })
                .toList();
    }
}
