/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.query.type;

import static org.geysermc.databaseutils.processor.util.CollectionUtils.join;
import static org.geysermc.databaseutils.processor.util.CollectionUtils.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.geysermc.databaseutils.processor.action.Action;
import org.geysermc.databaseutils.processor.info.ColumnInfo;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.query.KeywordsReadResult;
import org.geysermc.databaseutils.processor.query.section.by.keyword.EqualsKeyword;
import org.geysermc.databaseutils.processor.util.InvalidRepositoryException;
import org.geysermc.databaseutils.processor.util.TypeUtils;

public class ParametersTypeInfo {
    private final ExecutableElement element;
    private final boolean isSelf;
    private final TypeMirror elementType;
    private final boolean isSelfCollection;
    private final boolean isUnique;
    private final List<ColumnInfo> remaining;

    public ParametersTypeInfo(
            ExecutableElement element,
            KeywordsReadResult readResult,
            Action action,
            EntityInfo entityInfo,
            TypeUtils typeUtils) {
        this.element = element;
        this.isSelf = isSelf(entityInfo.asType(), typeUtils);
        this.elementType = elementType(typeUtils);
        this.isSelfCollection = elementType != null && typeUtils.isAssignable(elementType, entityInfo.asType());
        this.isUnique = isUnique(readResult, entityInfo);
        this.remaining = remaining(element, readResult, action, entityInfo, typeUtils);
    }

    public ExecutableElement element() {
        return element;
    }

    public boolean isSelf() {
        return isSelf;
    }

    public TypeMirror elementType() {
        return elementType;
    }

    public boolean isSelfCollection() {
        return isSelfCollection;
    }

    public boolean isUnique() {
        return isUnique;
    }

    public CharSequence name(int index) {
        return element.getParameters().get(index).getSimpleName();
    }

    public CharSequence firstName() {
        return name(0);
    }

    public List<CharSequence> names() {
        return map(element.getParameters(), VariableElement::getSimpleName);
    }

    public boolean hasParameters() {
        return !element.getParameters().isEmpty();
    }

    public boolean isAnySelf() {
        return isSelf() || isSelfCollection();
    }

    public boolean isNoneOrAnySelf() {
        return element.getParameters().isEmpty() || isAnySelf();
    }

    /**
     * Returns the remaining parameters, which each represent a column. The parameter name has to be identical to the
     * column name.
     * @return the remaining parameters, or an empty collection if the specified action does not allow this
     */
    public List<ColumnInfo> remaining() {
        return remaining;
    }

    private boolean isSelf(TypeMirror entityType, TypeUtils typeUtils) {
        if (element.getParameters().size() != 1) {
            return false;
        }
        var first = element.getParameters().get(0);
        return typeUtils.isAssignable(first.asType(), entityType);
    }

    private TypeMirror elementType(TypeUtils typeUtils) {
        if (element.getParameters().size() != 1) {
            return null;
        }
        var first = element.getParameters().get(0);
        if (!typeUtils.isAssignable(first.asType(), Collection.class)) {
            return null;
        }
        return ((DeclaredType) first.asType()).getTypeArguments().get(0);
    }

    private boolean isUnique(KeywordsReadResult readResult, EntityInfo entityInfo) {
        if (isAnySelf()) {
            return true;
        }

        var bySection = readResult.bySection();
        if (bySection == null) {
            return false;
        }

        var remainingKeys = entityInfo.keyColumns();
        for (var variable : bySection.variables()) {
            // we can only remove a remaining key if the value is an exact match (equals)
            if (!(variable.keyword() instanceof EqualsKeyword)) {
                continue;
            }
            remainingKeys.removeIf(column -> column.name().contentEquals(variable.columnName()));
        }
        return remainingKeys.isEmpty();
    }

    private List<ColumnInfo> remaining(
            ExecutableElement element,
            KeywordsReadResult readResult,
            Action action,
            EntityInfo entityInfo,
            TypeUtils typeUtils) {
        if (!action.remainingParametersAsColumns()) {
            return Collections.emptyList();
        }

        int requiredInputCount = 0;
        var bySection = readResult.bySection();
        if (bySection != null) {
            requiredInputCount = bySection.variables().stream()
                    .mapToInt(value -> value.keyword().inputCount())
                    .sum();
        }

        // any self = first parameter is self. So all the bySection variables can be filled with just that parameter
        if (isAnySelf()) {
            requiredInputCount = 1;
        }

        var remaining = new ArrayList<ColumnInfo>();
        var unknown = new ArrayList<CharSequence>();
        for (int i = requiredInputCount; i < element.getParameters().size(); i++) {
            var parameter = element.getParameters().get(i);
            var column = entityInfo.columnFor(parameter.getSimpleName());
            if (column == null) {
                unknown.add(parameter.getSimpleName());
                continue;
            }
            if (!typeUtils.isAssignable(parameter.asType(), column.asType())) {
                unknown.add(parameter.getSimpleName());
            }
            remaining.add(column);
        }

        if (!unknown.isEmpty()) {
            throw new InvalidRepositoryException(
                    "Did not recognise %s as matches (either name or type) of %s columns, for method %s",
                    join(remaining), entityInfo.typeName(), element);
        }
        return remaining;
    }
}
