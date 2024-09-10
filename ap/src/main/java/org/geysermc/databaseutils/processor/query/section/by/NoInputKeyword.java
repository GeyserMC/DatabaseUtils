/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.query.section.by;

import java.util.List;

public abstract class NoInputKeyword extends InputKeyword {
    @Override
    public List<List<Class<?>>> acceptedInputs() {
        return List.of();
    }
}
