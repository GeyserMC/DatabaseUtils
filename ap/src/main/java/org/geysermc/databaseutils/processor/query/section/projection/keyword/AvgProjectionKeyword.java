/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.query.section.projection.keyword;

import org.geysermc.databaseutils.processor.query.section.projection.ProjectionKeyword;
import org.geysermc.databaseutils.processor.query.section.projection.ProjectionKeywordCategory;

public class AvgProjectionKeyword extends ProjectionKeyword {
    public static final AvgProjectionKeyword INSTANCE = new AvgProjectionKeyword();

    private AvgProjectionKeyword() {
        super("Avg", ProjectionKeywordCategory.SUMMARY);
    }
}
