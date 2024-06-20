/*
 * Copyright (c) 2024 GeyserMC <https://geysermc.org>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
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
    private final boolean async;
    private final TypeMirror type;
    private final boolean isVoid;
    private final TypeMirror elementType;

    public ReturnTypeInfo(boolean async, TypeMirror type, TypeUtils typeUtils) {
        this.async = async;
        this.type = type;
        this.isVoid = isVoid(typeUtils);
        this.elementType = elementType(typeUtils);
    }

    public boolean async() {
        return async;
    }

    public TypeMirror type() {
        return type;
    }

    public boolean isVoid() {
        return isVoid;
    }

    public TypeMirror elementType() {
        return elementType;
    }

    public boolean isCollection() {
        return elementType() != null;
    }

    private boolean isVoid(TypeUtils typeUtils) {
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
