/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.util;

import com.google.auto.common.MoreTypes;
import com.squareup.javapoet.ClassName;
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

    public ClassName collectionImplementationFor(TypeMirror returnType) {
        if (isType(Set.class, returnType)) {
            return ClassName.get(HashSet.class);
        }
        if (isType(List.class, returnType)) {
            return ClassName.get(ArrayList.class);
        }
        var implType = MoreTypes.asTypeElement(returnType);
        for (Element element : implType.getEnclosedElements()) {
            if (element.getSimpleName().contentEquals("<init>")) {
                var constructor = (ExecutableElement) element;
                if (constructor.getParameters().isEmpty()
                        && constructor.getModifiers().contains(Modifier.PUBLIC)) {
                    return ClassName.get(implType);
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
