/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.action;

import com.squareup.javapoet.MethodSpec;
import javax.lang.model.type.TypeMirror;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.query.QueryContext;
import org.geysermc.databaseutils.processor.type.RepositoryGenerator;
import org.geysermc.databaseutils.processor.util.InvalidRepositoryException;
import org.geysermc.databaseutils.processor.util.TypeUtils;

final class DeleteAction extends Action {
    DeleteAction() {
        super("delete", false, true, true, true);
    }

    @Override
    protected void addToSingle(RepositoryGenerator generator, QueryContext context, MethodSpec.Builder spec) {
        generator.addDelete(context, spec);
    }

    @Override
    protected boolean validateSingle(
            EntityInfo info, CharSequence methodName, TypeMirror returnType, TypeUtils typeUtils) {
        if (!typeUtils.isType(Void.class, returnType)
                && !typeUtils.isType(Integer.class, returnType)
                && !typeUtils.isType(Boolean.class, returnType)) {
            throw new InvalidRepositoryException(
                    "Expected Void, Integer or Boolean as return type for %s, got %s", methodName, returnType);
        }
        return true;
    }
}
