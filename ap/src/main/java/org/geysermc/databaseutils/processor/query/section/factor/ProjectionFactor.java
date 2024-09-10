/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.query.section.factor;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.databaseutils.processor.query.section.projection.ProjectionKeyword;

public record ProjectionFactor(@Nullable ProjectionKeyword keyword, @Nullable String columnName) implements Factor {
    public ProjectionFactor {
        if (columnName == null && keyword == null) {
            throw new IllegalArgumentException("Either the columnName or keyword must be specified");
        }
    }
}
