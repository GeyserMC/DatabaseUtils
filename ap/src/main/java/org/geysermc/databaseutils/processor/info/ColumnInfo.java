/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.info;

import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.geysermc.databaseutils.meta.Length;

/**
 * @param maxLength Returns the max length as provided by {@link Length} (either directly or from a TypeCodec), or -1 if no limit is provided
 */
public record ColumnInfo(Name name, TypeElement type, VariableElement variable, int maxLength) {
    public TypeMirror asType() {
        return type.asType();
    }

    public Name typeName() {
        return type.getQualifiedName();
    }
}
