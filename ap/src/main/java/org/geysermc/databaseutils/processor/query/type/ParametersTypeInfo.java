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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.geysermc.databaseutils.processor.util.TypeUtils;

public class ParametersTypeInfo {
    private final ExecutableElement element;
    private final boolean isSelf;
    private final TypeMirror elementType;
    private final boolean isSelfCollection;

    public ParametersTypeInfo(ExecutableElement element, CharSequence entityType, TypeUtils typeUtils) {
        this.element = element;
        this.isSelf = isSelf(entityType, typeUtils);
        this.elementType = elementType(typeUtils);
        this.isSelfCollection = elementType != null && typeUtils.isAssignable(elementType, entityType);
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

    public boolean hasParameters() {
        return !element.getParameters().isEmpty();
    }

    public boolean isAnySelf() {
        return isSelf() || isSelfCollection();
    }

    public boolean isNoneOrAnySelf() {
        return element.getParameters().isEmpty() || isAnySelf();
    }

    private boolean isSelf(CharSequence entityType, TypeUtils typeUtils) {
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
