/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.info;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.lang.model.element.TypeElement;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.databaseutils.processor.query.section.by.keyword.EqualsKeyword;
import org.geysermc.databaseutils.processor.query.section.factor.Factor;
import org.geysermc.databaseutils.processor.query.section.factor.VariableByFactor;

public record EntityInfo(
        String name, TypeElement type, List<ColumnInfo> columns, List<IndexInfo> indexes, List<CharSequence> keys) {
    public ColumnInfo columnFor(CharSequence columnName) {
        for (ColumnInfo column : columns) {
            if (column.name().contentEquals(columnName)) {
                return column;
            }
        }
        return null;
    }

    public CharSequence typeName() {
        return type.getQualifiedName();
    }

    public List<ColumnInfo> keyColumns() {
        return keys.stream().map(this::columnFor).collect(Collectors.toList());
    }

    public List<ColumnInfo> notKeyColumns() {
        return columns().stream()
                .filter(column -> !keys.contains(column.name()))
                .toList();
    }

    public List<Factor> keyColumnsAsFactors(@Nullable Factor separator, @NonNull CharSequence parameterName) {
        return asFactors(keyColumns(), separator, parameterName);
    }

    public List<Factor> notKeyColumnsAsFactors(@Nullable Factor separator, @NonNull CharSequence parameterName) {
        return asFactors(notKeyColumns(), separator, parameterName);
    }

    private List<Factor> asFactors(
            @NonNull List<ColumnInfo> columns, @Nullable Factor separator, @Nullable CharSequence parameterName) {
        var factors = new ArrayList<Factor>();
        for (ColumnInfo column : columns) {
            if (!factors.isEmpty() && separator != null) {
                factors.add(separator);
            }

            if (parameterName != null) {
                var name = "%s.%s()".formatted(parameterName, column.name());
                factors.add(new VariableByFactor(column.name(), new EqualsKeyword().addParameterName(name)));
            }
        }
        return factors;
    }
}
