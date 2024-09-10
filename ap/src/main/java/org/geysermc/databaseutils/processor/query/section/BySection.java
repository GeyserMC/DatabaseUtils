/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.query.section;

import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.databaseutils.processor.query.section.factor.Factor;
import org.geysermc.databaseutils.processor.query.section.factor.VariableByFactor;

public record BySection(@NonNull List<@NonNull Factor> factors) {
    public List<VariableByFactor> variables() {
        return factors.stream()
                .<VariableByFactor>mapMulti((section, result) -> {
                    if (section instanceof VariableByFactor variable) {
                        result.accept(variable);
                    }
                })
                .toList();
    }
}
