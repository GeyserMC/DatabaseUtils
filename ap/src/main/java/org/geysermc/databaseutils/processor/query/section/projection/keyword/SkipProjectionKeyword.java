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

public class SkipProjectionKeyword extends ProjectionKeyword {
    private int offset;

    public SkipProjectionKeyword() {
        super("Skip[1-9][0-9]*", ProjectionKeywordCategory.OFFSET);
    }

    public SkipProjectionKeyword(int offset) {
        this();
        offset(offset);
    }

    public int offset() {
        return offset;
    }

    public void offset(int limit) {
        this.offset = limit;
    }

    public void setValue(@NonNull String fullKeyword) {
        offset(Integer.parseInt(fullKeyword.substring(3)));
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }
        return ((SkipProjectionKeyword) o).offset == offset;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name(), offset);
    }
}
