/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.query.section.factor;

import org.checkerframework.checker.nullness.qual.NonNull;

public interface VariableFactor extends Factor {
    @NonNull CharSequence columnName();
}
