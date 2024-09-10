/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.query.type;

import java.util.Collection;
import java.util.Objects;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.geysermc.databaseutils.processor.util.TypeUtils;

public final class ReturnTypeInfo {
    private final TypeUtils typeUtils;
    private final boolean async;
    private final TypeMirror type;
    private final TypeMirror selfType;
    private final TypeMirror elementType;

    public ReturnTypeInfo(boolean async, TypeMirror type, TypeMirror selfType, TypeUtils typeUtils) {
        this.typeUtils = typeUtils;
        this.async = async;
        this.type = type;
        this.selfType = selfType;
        this.elementType = elementType(typeUtils);
    }

    public boolean async() {
        return async;
    }

    public TypeMirror type() {
        return type;
    }

    public TypeMirror elementType() {
        return elementType;
    }

    public TypeMirror elementTypeOrType() {
        return elementType != null ? elementType : type;
    }

    public boolean isSelf() {
        return typeUtils.isType(selfType, type);
    }

    public boolean isCollection() {
        return elementType() != null;
    }

    public boolean isSelfCollection() {
        return isCollection() && typeUtils.isType(selfType, elementType);
    }

    public boolean isAnySelf() {
        return isSelf() || isSelfCollection();
    }

    public boolean isVoid() {
        if (type.getKind() == TypeKind.VOID) {
            return true;
        }
        if (type.getKind() == TypeKind.DECLARED) {
            return typeUtils.isType(Void.class, type);
        }
        return false;
    }

    private TypeMirror elementType(TypeUtils typeUtils) {
        if (type.getKind() != TypeKind.DECLARED) {
            return null;
        }
        if (!typeUtils.isAssignable(type, Collection.class)) {
            return null;
        }
        return ((DeclaredType) type).getTypeArguments().get(0);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ReturnTypeInfo) obj;
        return Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    @Override
    public String toString() {
        return "ReturnTypeInfo[" + "type=" + type + ']';
    }
}
