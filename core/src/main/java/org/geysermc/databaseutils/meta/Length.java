/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.checker.index.qual.Positive;

@Target(ElementType.FIELD)
public @interface Length {
    /**
     * The maximum length (in bytes) that the given column can be. Note that for strings this mean that you have to
     * keep the chosen charset (by default almost always UTF-8) into account. A single UTF-8 character can vary from 1
     * to 4 bytes per character.
     */
    @Positive int max() default 0;
}
