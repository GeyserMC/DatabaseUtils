/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
@Repeatable(Indexes.class)
public @interface Index {
    String name() default "";

    String[] columns();

    boolean unique() default false;

    IndexDirection direction() default IndexDirection.ASCENDING;

    enum IndexDirection {
        ASCENDING,
        DESCENDING
    }
}
