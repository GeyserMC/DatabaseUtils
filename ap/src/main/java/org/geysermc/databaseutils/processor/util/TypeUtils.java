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
package org.geysermc.databaseutils.processor.util;

import com.google.auto.common.MoreTypes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public final class TypeUtils {
    private final Types typeUtils;
    private final Elements elementUtils;

    public TypeUtils(Types typeUtils, Elements elementUtils) {
        this.typeUtils = typeUtils;
        this.elementUtils = elementUtils;
    }

    public Types typeUtils() {
        return typeUtils;
    }

    public Elements elementUtils() {
        return elementUtils;
    }

    public TypeElement elementFor(CharSequence name) {
        return elementUtils.getTypeElement(name);
    }

    public TypeElement elementFor(Class<?> clazz) {
        return elementFor(clazz.getCanonicalName());
    }

    public TypeMirror toBoxedMirror(TypeMirror mirror) {
        if (mirror.getKind() == TypeKind.ERROR) {
            throw new InvalidRepositoryException(
                    "Could not resolve a specific class! Please make sure you added all correct imports");
        }
        if (mirror.getKind().isPrimitive()) {
            return typeUtils.boxedClass(MoreTypes.asPrimitiveType(mirror)).asType();
        }
        if (mirror.getKind() == TypeKind.VOID) {
            return elementFor(Void.class).asType();
        }
        return mirror;
    }

    public TypeElement toBoxedTypeElement(TypeMirror mirror) {
        if (mirror.getKind().isPrimitive() || mirror.getKind() == TypeKind.VOID) {
            return typeUtils.boxedClass(MoreTypes.asPrimitiveType(mirror));
        }
        return MoreTypes.asTypeElement(mirror);
    }

    public boolean isAssignable(TypeMirror impl, TypeMirror base) {
        return typeUtils.isAssignable(impl, base);
    }

    public boolean isAssignable(CharSequence impl, TypeMirror base) {
        return isAssignable(elementFor(impl).asType(), typeUtils.erasure(base));
    }

    public boolean isAssignable(Class<?> impl, TypeMirror base) {
        return isAssignable(impl.getCanonicalName(), base);
    }

    public boolean isAssignable(TypeMirror impl, CharSequence base) {
        return isAssignable(typeUtils.erasure(impl), elementFor(base).asType());
    }

    public boolean isAssignable(TypeMirror impl, Class<?> base) {
        return isAssignable(impl, base.getCanonicalName());
    }

    public CharSequence canonicalName(TypeMirror mirror) {
        return toBoxedTypeElement(mirror).getQualifiedName();
    }

    public CharSequence collectionImplementationFor(TypeMirror returnType) {
        if (isType(Set.class, returnType)) {
            return HashSet.class.getCanonicalName();
        }
        if (isType(List.class, returnType)) {
            return ArrayList.class.getCanonicalName();
        }
        var implType = MoreTypes.asTypeElement(returnType);
        for (Element element : implType.getEnclosedElements()) {
            if (element.getSimpleName().contentEquals("<init>")) {
                var constructor = (ExecutableElement) element;
                if (constructor.getParameters().isEmpty()
                        && constructor.getModifiers().contains(Modifier.PUBLIC)) {
                    return implType.getQualifiedName();
                }
            }
        }
        throw new IllegalStateException("Cannot find an usable implementation for " + returnType);
    }

    public boolean isType(Class<?> clazz, TypeMirror mirror) {
        return isType(clazz.getCanonicalName(), mirror);
    }

    public static boolean isType(CharSequence expected, TypeElement actual) {
        return actual.getQualifiedName().contentEquals(expected);
    }

    public boolean isType(CharSequence expected, TypeMirror actual) {
        return isType(elementFor(expected).asType(), actual);
    }

    public boolean isType(TypeMirror expected, TypeMirror actual) {
        return typeUtils.isSameType(typeUtils.erasure(expected), typeUtils.erasure(actual));
    }

    public static boolean isType(Class<?> clazz, TypeElement element) {
        return isType(clazz, element.getQualifiedName());
    }

    public static boolean isType(Class<?> clazz, CharSequence canonicalName) {
        return clazz.getCanonicalName().contentEquals(canonicalName);
    }

    public static boolean isType(CharSequence expected, CharSequence actual) {
        return CharSequence.compare(expected, actual) == 0;
    }

    public static String packageNameFor(Name className) {
        return packageNameFor(className.toString());
    }

    public static String packageNameFor(String className) {
        return className.substring(0, className.lastIndexOf('.'));
    }

    public static boolean needsTypeCodec(Name className) {
        // See README
        return Set.of(
                        Boolean.class,
                        Byte.class,
                        Short.class,
                        Character.class,
                        Integer.class,
                        Long.class,
                        Float.class,
                        Double.class,
                        String.class,
                        Byte[].class)
                .stream()
                .filter(clazz -> className.contentEquals(clazz.getCanonicalName()))
                .findAny()
                .isEmpty();
    }

    public boolean isWholeNumberType(TypeMirror mirror) {
        return isType(Byte.class, mirror)
                || isType(Short.class, mirror)
                || isType(Integer.class, mirror)
                || isType(Long.class, mirror);
    }
}
