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
import java.util.Set;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
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

    public TypeElement toBoxedTypeElement(TypeMirror mirror) {
        if (mirror.getKind().isPrimitive()) {
            return typeUtils.boxedClass(MoreTypes.asPrimitiveType(mirror));
        }
        return MoreTypes.asTypeElement(mirror);
    }

    public boolean isAssignable(TypeMirror impl, TypeMirror base) {
        return typeUtils.isAssignable(impl, base);
    }

    public boolean isAssignable(CharSequence impl, TypeMirror base) {
        return isAssignable(elementUtils.getTypeElement(impl).asType(), base);
    }

    public boolean isAssignable(Class<?> impl, TypeMirror base) {
        return isAssignable(impl.getCanonicalName(), base);
    }

    public boolean isAssignable(TypeMirror impl, CharSequence base) {
        return isAssignable(impl, elementUtils.getTypeElement(base).asType());
    }

    public boolean isAssignable(TypeMirror impl, Class<?> base) {
        return isAssignable(impl, base.getCanonicalName());
    }

    public CharSequence canonicalName(TypeMirror mirror) {
        return toBoxedTypeElement(mirror).getQualifiedName();
    }

    public static boolean isType(Class<?> clazz, TypeElement element) {
        return isType(clazz, element.getQualifiedName());
    }

    public static boolean isType(Class<?> clazz, CharSequence canonicalName) {
        return clazz.getCanonicalName().contentEquals(canonicalName);
    }

    public static boolean isType(CharSequence name, TypeElement element) {
        return element.getQualifiedName().contentEquals(name);
    }

    public static boolean isType(CharSequence expected, TypeMirror actual) {
        return isType(expected, MoreTypes.asTypeElement(actual));
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

    public static boolean isWholeNumberType(TypeElement element) {
        return isType(Byte.class, element)
                || isType(Short.class, element)
                || isType(Integer.class, element)
                || isType(Long.class, element);
    }
}
