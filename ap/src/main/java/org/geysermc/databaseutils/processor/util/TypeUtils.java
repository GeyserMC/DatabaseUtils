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
import javax.lang.model.util.Types;

public final class TypeUtils {
    private TypeUtils() {}

    public static TypeElement toBoxedTypeElement(TypeMirror mirror, Types typeUtils) {
        if (mirror.getKind().isPrimitive()) {
            return typeUtils.boxedClass(MoreTypes.asPrimitiveType(mirror));
        }
        return MoreTypes.asTypeElement(mirror);
    }

    public static boolean isTypeOf(Class<?> clazz, TypeElement element) {
        return isTypeOf(clazz, element.getQualifiedName());
    }

    public static boolean isTypeOf(Class<?> clazz, Name canonicalName) {
        return canonicalName.contentEquals(clazz.getCanonicalName());
    }

    public static boolean isTypeOf(CharSequence name, TypeElement element) {
        return element.getQualifiedName().contentEquals(name);
    }

    public static boolean isTypeOf(CharSequence expected, TypeMirror actual) {
        return isTypeOf(expected, MoreTypes.asTypeElement(actual));
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
}
