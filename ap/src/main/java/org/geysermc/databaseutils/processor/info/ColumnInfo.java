/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.info;

import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;

public record ColumnInfo(Name name, TypeElement type, Name typeName) {}
