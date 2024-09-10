/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.action;

import com.squareup.javapoet.MethodSpec;
import org.geysermc.databaseutils.processor.query.QueryContext;
import org.geysermc.databaseutils.processor.type.RepositoryGenerator;
import org.geysermc.databaseutils.processor.util.InvalidRepositoryException;

final class UpdateAction extends Action {
    UpdateAction() {
        // todo add allowReturnAnySelfOrColumn support
        super("update", true, false, true, true);
    }

    @Override
    protected void addToSingle(RepositoryGenerator generator, QueryContext context, MethodSpec.Builder spec) {
        generator.addUpdate(context, spec);
    }

    @Override
    protected boolean validateEither(QueryContext context, boolean passedCustomValidation) {
        if (!context.parametersInfo().isAnySelf()
                && context.parametersInfo().remaining().isEmpty()) {
            throw new InvalidRepositoryException(
                    "Expected additional parameters for the changed columns (e.g. updateById(id, title) to change the title for a given id) for %s",
                    context.method());
        }
        return false;
    }
}
