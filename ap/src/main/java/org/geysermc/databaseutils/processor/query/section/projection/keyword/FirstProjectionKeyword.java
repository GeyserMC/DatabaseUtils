/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.query.section.projection.keyword;

import org.geysermc.databaseutils.processor.query.section.projection.ProjectionKeyword;
import org.geysermc.databaseutils.processor.query.section.projection.ProjectionKeywordCategory;

public class FirstProjectionKeyword extends ProjectionKeyword {
    public static final FirstProjectionKeyword INSTANCE = new FirstProjectionKeyword();

    private FirstProjectionKeyword() {
        super("First", ProjectionKeywordCategory.LIMIT);
    }
}
