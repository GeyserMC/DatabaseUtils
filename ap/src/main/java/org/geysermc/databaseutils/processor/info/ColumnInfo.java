/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.info;

import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

public record ColumnInfo(Name name, TypeElement type) {
    public TypeMirror asType() {
        return type.asType();
    }

    public Name typeName() {
        return type.getQualifiedName();
    }
}
