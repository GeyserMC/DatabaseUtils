/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.query.section.projection.keyword;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.databaseutils.processor.query.section.projection.ProjectionKeyword;
import org.geysermc.databaseutils.processor.query.section.projection.ProjectionKeywordCategory;

public class TopProjectionKeyword extends ProjectionKeyword {
    private int limit;

    public TopProjectionKeyword() {
        super("Top[1-9][0-9]*", ProjectionKeywordCategory.LIMIT);
    }

    public TopProjectionKeyword(int limit) {
        this();
        limit(limit);
    }

    public int limit() {
        return limit;
    }

    public void limit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than 0");
        }
        this.limit = limit;
    }

    public void setValue(@NonNull String fullKeyword) {
        limit(Integer.parseInt(fullKeyword.substring(3)));
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }
        return ((TopProjectionKeyword) o).limit == limit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name(), limit);
    }
}
