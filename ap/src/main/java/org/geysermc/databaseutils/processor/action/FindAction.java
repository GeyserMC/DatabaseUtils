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

final class FindAction extends Action {
    FindAction() {
        super(
                "find",
                false,
                true,
                true,
                false,
                ProjectionKeywordCategory.UNIQUE,
                ProjectionKeywordCategory.SUMMARY,
                ProjectionKeywordCategory.LIMIT);
    }

    @Override
    protected boolean validateEither(QueryContext context, boolean passedCustomValidation) {
        if (passedCustomValidation) {
            return true;
        }
        if (!context.returnInfo().isAnySelf()) {
            throw new InvalidRepositoryException(
                    "Expected %s as return type for %s, got %s",
                    context.entityTypeName(), context.methodName(), context.returnType());
        }
        return true;
    }

    @Override
    protected boolean validateCollection(QueryContext context, boolean passedCustomValidation) {
        return false;
    }

    @Override
    public void addToSingle(RepositoryGenerator generator, QueryContext context, MethodSpec.Builder spec) {
        generator.addFind(context, spec);
    }
}
