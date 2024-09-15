/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.query;

import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.geysermc.databaseutils.processor.info.ColumnInfo;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.query.section.ProjectionSection;
import org.geysermc.databaseutils.processor.query.section.factor.Factor;
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

    public CharSequence methodName() {
        return parametersInfo.element().getSimpleName();
    }

    public ExecutableElement method() {
        return parametersInfo.element();
    }

    public TypeMirror returnType() {
        return returnInfo.type();
    }

    public TypeMirror countableReturnType() {
        if (typeUtils.isWholeNumberType(returnType())) {
            return typeUtils.unboxType(returnType());
        }
        // for Boolean and everything else
        return typeUtils.typeUtils().getPrimitiveType(TypeKind.LONG);
    }

    public ProjectionSection projection() {
        return result.projection();
    }

    public boolean hasProjection() {
        return projection() != null;
    }

    public boolean hasProjectionColumnName() {
        return projection() != null && projection().columnName() != null;
    }

    public ColumnInfo projectionColumnInfo() {
        return entityInfo.columnFor(projection().columnName());
    }

    public boolean hasParameters() {
        return parametersInfo.hasParameters();
    }
}
