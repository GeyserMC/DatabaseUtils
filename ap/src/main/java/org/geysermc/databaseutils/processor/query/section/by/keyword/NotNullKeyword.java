/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.query.section.by.keyword;

import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.databaseutils.processor.query.section.by.NoInputKeyword;

public final class NotNullKeyword extends NoInputKeyword {
    public static final NotNullKeyword INSTANCE = new NotNullKeyword();

    private NotNullKeyword() {}

    @Override
    public @NonNull List<@NonNull String> names() {
        return List.of("IsNotNull", "NotNull");
    }
}
