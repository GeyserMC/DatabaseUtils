/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.query.section.factor;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.databaseutils.processor.query.section.by.InputKeyword;
import org.geysermc.databaseutils.processor.query.section.by.keyword.EqualsKeyword;

public record VariableByFactor(@NonNull CharSequence columnName, @NonNull InputKeyword keyword)
        implements VariableFactor {
    public VariableByFactor(@NonNull CharSequence columnName, @Nullable InputKeyword keyword) {
        this.columnName = columnName;
        if (keyword == null) {
            keyword = new EqualsKeyword();
        }
        this.keyword = keyword;
    }

    public VariableByFactor(@NonNull CharSequence columnName, @NonNull CharSequence parameterName) {
        this(columnName);
        this.keyword.addParameterName(parameterName);
    }

    public VariableByFactor(@NonNull CharSequence name) {
        this(name, (InputKeyword) null);
    }
}
