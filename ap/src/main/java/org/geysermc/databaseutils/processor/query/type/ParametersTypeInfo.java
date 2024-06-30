/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.query.type;

import java.util.Collection;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.geysermc.databaseutils.processor.util.TypeUtils;

public class ParametersTypeInfo {
    private final ExecutableElement element;
    private final boolean isSelf;
    private final TypeMirror elementType;
    private final boolean isSelfCollection;
    private final boolean hasColumnParameter;

    public ParametersTypeInfo(
            ExecutableElement element, TypeMirror entityType, TypeUtils typeUtils, boolean hasColumnParameter) {
        this.element = element;
        this.isSelf = isSelf(entityType, typeUtils);
        this.elementType = elementType(typeUtils);
        this.isSelfCollection = elementType != null && typeUtils.isAssignable(elementType, entityType);
        this.hasColumnParameter = hasColumnParameter;
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

    public CharSequence name(int index) {
        return element.getParameters().get(index).getSimpleName();
    }

    public CharSequence columnParameter() {
        return hasColumnParameter ? name(0) : null;
    }

    public CharSequence firstName() {
        return name(hasColumnParameter ? 1 : 0);
    }

    public boolean hasValueParameters() {
        return element.getParameters().size() > (hasColumnParameter ? 1 : 0);
    }

    public boolean isAnySelf() {
        return isSelf() || isSelfCollection();
    }

    public boolean isNoneOrAnySelf() {
        return element.getParameters().isEmpty() || isAnySelf();
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
}
