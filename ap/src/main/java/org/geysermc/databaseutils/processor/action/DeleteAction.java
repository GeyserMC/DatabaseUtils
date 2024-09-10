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

final class DeleteAction extends Action {
    DeleteAction() {
        super("delete", true, true, true, false);
    }

    @Override
    protected void addToSingle(RepositoryGenerator generator, QueryContext context, MethodSpec.Builder spec) {
        generator.addDelete(context, spec);
    }

    @Override
    protected boolean validateCollection(QueryContext context, boolean passedCustomValidation) {
        return context.returnInfo().isSelfCollection();
    }

    @Override
    protected boolean validateSingle(QueryContext context, boolean passedCustomValidation) {
        if (context.returnInfo().isSelf()
                && (context.projection() == null || !context.projection().first())
                && !context.parametersInfo().isUnique()) {
            throw new InvalidRepositoryException(
                    "Please make deleting the first match explicit by adding the 'first' projection (e.g. deleteFirstByTitle) to %s, as the current query can result in multiple matches.",
                    context.methodName());
        }

        if (!context.returnInfo().isSelf()
                && !context.typeUtils().isType(Void.class, context.returnType())
                && !context.typeUtils().isType(Integer.class, context.returnType())
                && !context.typeUtils().isType(Boolean.class, context.returnType())) {
            throw new InvalidRepositoryException(
                    "Expected Void, Integer, Boolean or %s as return type for %s, got %s",
                    context.entityTypeName(), context.methodName(), context.returnType());
        }
        return true;
    }
}
