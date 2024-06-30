/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.query;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.geysermc.databaseutils.processor.info.ColumnInfo;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.query.section.ProjectionSection;
import org.geysermc.databaseutils.processor.query.section.factor.Factor;
import org.geysermc.databaseutils.processor.query.section.factor.VariableByFactor;
import org.geysermc.databaseutils.processor.query.type.ParametersTypeInfo;
import org.geysermc.databaseutils.processor.query.type.ReturnTypeInfo;
import org.geysermc.databaseutils.processor.util.TypeUtils;

public record QueryContext(
        EntityInfo entityInfo,
        KeywordsReadResult result,
        ParametersTypeInfo parametersInfo,
        ReturnTypeInfo returnInfo,
        TypeUtils typeUtils) {

    public String tableName() {
        return entityInfo.name();
    }

    public CharSequence entityTypeName() {
        return entityInfo.typeName();
    }

    public TypeElement entityType() {
        return entityInfo.type();
    }

    public List<ColumnInfo> columns() {
        return entityInfo.columns();
    }

    public ColumnInfo columnFor(CharSequence columnName) {
        for (ColumnInfo column : columns()) {
            if (column.name().contentEquals(columnName)) {
                return column;
            }
        }
        return null;
    }

    public boolean hasBySection() {
        return result.bySection() != null;
    }

    public @MonotonicNonNull List<Factor> bySectionFactors() {
        return result.bySection() != null ? result.bySection().factors() : null;
    }

    public List<VariableByFactor> byVariables() {
        if (bySectionFactors() == null) {
            return Collections.emptyList();
        }

        return bySectionFactors().stream()
                .flatMap(section -> {
                    if (section instanceof VariableByFactor variable) {
                        return Stream.of(variable);
                    }
                    return null;
                })
                .toList();
    }

    public TypeMirror returnType() {
        return returnInfo.type();
    }

    public ProjectionSection projection() {
        return result.projection();
    }
}
