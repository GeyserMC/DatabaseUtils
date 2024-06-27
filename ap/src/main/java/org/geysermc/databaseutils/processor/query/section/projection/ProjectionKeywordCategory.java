/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.query.section.projection;

import java.util.Locale;

public enum ProjectionKeywordCategory {
    UNIQUE(false),
    SUMMARY(true),
    LIMIT(false),
    OFFSET(false);

    private final boolean requiresColumn;

    ProjectionKeywordCategory(boolean requiresColumn) {
        this.requiresColumn = requiresColumn;
    }

    public boolean requiresColumn() {
        return requiresColumn;
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
