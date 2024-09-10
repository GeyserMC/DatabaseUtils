/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.action;

import com.squareup.javapoet.MethodSpec;
import org.geysermc.databaseutils.processor.query.QueryContext;
import org.geysermc.databaseutils.processor.query.section.projection.ProjectionKeywordCategory;
import org.geysermc.databaseutils.processor.type.RepositoryGenerator;
import org.geysermc.databaseutils.processor.util.InvalidRepositoryException;

final class ExistsAction extends Action {
    ExistsAction() {
        super("exists", false, false, true, false, ProjectionKeywordCategory.UNIQUE);
    }

    @Override
    protected boolean validateSingle(QueryContext context, boolean passedCustomValidation) {
        if (!context.typeUtils().isType(Boolean.class, context.returnType())) {
            throw new InvalidRepositoryException(
                    "Expected Boolean as return type for %s, got %s", context.methodName(), context.returnType());
        }
        return true;
    }

    @Override
    public void addToSingle(RepositoryGenerator generator, QueryContext context, MethodSpec.Builder spec) {
        generator.addExists(context, spec);
    }
}
