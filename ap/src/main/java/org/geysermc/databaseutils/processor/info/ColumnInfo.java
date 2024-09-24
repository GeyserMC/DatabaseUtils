/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.info;

import java.lang.annotation.Annotation;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.geysermc.databaseutils.meta.Length;

public record ColumnInfo(Name name, TypeElement type, VariableElement variable) {
    public TypeMirror asType() {
        return type.asType();
    }

    public Name typeName() {
        return type.getQualifiedName();
    }

    public <T extends Annotation> T annotation(Class<T> annotationClass) {
        return variable.getAnnotation(annotationClass);
    }

    /**
     * Returns the max length as provided by {@link Length}, or -1 if no limit is provided
     */
    public int maxLength() {
        var length = annotation(Length.class);
        return length != null ? length.max() : -1;
    }
}
