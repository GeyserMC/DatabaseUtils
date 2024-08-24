/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.action;

import com.squareup.javapoet.MethodSpec;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.query.QueryContext;
import org.geysermc.databaseutils.processor.query.section.projection.ProjectionKeywordCategory;
import org.geysermc.databaseutils.processor.type.RepositoryGenerator;
import org.geysermc.databaseutils.processor.util.InvalidRepositoryException;
import org.geysermc.databaseutils.processor.util.TypeUtils;

public abstract class Action {
    private final String actionType;
    private final boolean projectionColumnIsParameter;
    private final boolean allowSelfParameter;
    private final boolean allowReturnSelfCollection;
    private final boolean supportsFilter;
    private final List<ProjectionKeywordCategory> supportedProjectionCategories;

    protected Action(
            String actionType,
            boolean projectionColumnIsParameter,
            boolean allowSelfParameter,
            boolean allowReturnSelfCollection,
            boolean supportsFilter,
            ProjectionKeywordCategory... supportedProjectionCategories) {
        this.actionType = actionType;
        this.projectionColumnIsParameter = projectionColumnIsParameter;
        this.allowSelfParameter = allowSelfParameter;
        this.allowReturnSelfCollection = allowReturnSelfCollection;
        this.supportsFilter = supportsFilter;
        this.supportedProjectionCategories = List.of(supportedProjectionCategories);
    }

    public String actionType() {
        return actionType;
    }

    public boolean projectionColumnIsParameter() {
        return projectionColumnIsParameter;
    }

    public boolean allowSelfParameter() {
        return allowSelfParameter;
    }

    public boolean allowReturnSelfCollection() {
        return allowReturnSelfCollection;
    }

    public boolean supportsFilter() {
        return supportsFilter;
    }

    public List<ProjectionKeywordCategory> supportedProjectionCategories() {
        return supportedProjectionCategories;
    }

    protected abstract void addToSingle(RepositoryGenerator generator, QueryContext context, MethodSpec.Builder spec);

    protected boolean validateSingle(
            EntityInfo info, CharSequence methodName, TypeMirror returnType, TypeUtils typeUtils) {
        return typeUtils.isType(Void.class, returnType) || typeUtils.isWholeNumberType(returnType);
    }

    protected boolean validateCollection(
            EntityInfo info, CharSequence methodName, TypeMirror returnType, TypeUtils typeUtils) {
        throw new InvalidRepositoryException(
                "Collection return type (%s) is not supported for %s", returnType, actionType());
    }

    protected boolean validateEither(
            EntityInfo info, CharSequence methodName, TypeMirror returnType, boolean collection, TypeUtils typeUtils) {
        return false;
    }

    public void validate(
            EntityInfo info,
            CharSequence methodName,
            TypeMirror returnType,
            TypeUtils typeUtils,
            Consumer<TypeMirror> customValidation) {
        var passedCustomValidation = false;
        if (typeUtils.isAssignable(returnType, Collection.class)) {
            returnType = ((DeclaredType) returnType).getTypeArguments().get(0);

            if (!supportsFilter) {
                throw new InvalidRepositoryException("%s does not support a By section", actionType);
            }

            if (customValidation != null) {
                customValidation.accept(returnType);
                passedCustomValidation = true;
            }

            if (validateCollection(info, methodName, returnType, typeUtils)
                    || validateEither(info, methodName, returnType, true, typeUtils)) {
                return;
            }
        } else {
            if (customValidation != null) {
                customValidation.accept(returnType);
                passedCustomValidation = true;
            }

            if (validateEither(info, methodName, returnType, false, typeUtils)) {
                return;
            }
            if (validateSingle(info, methodName, returnType, typeUtils)) {
                return;
            }
        }

        if (!passedCustomValidation) {
            throw new InvalidRepositoryException("Unsupported return type %s for %s", returnType, methodName);
        }
    }

    public void addTo(List<RepositoryGenerator> generators, QueryContext context) {
        if (!context.hasBySection() && !context.parametersInfo().isNoneOrAnySelf()) {
            throw new InvalidRepositoryException(
                    "Expected at most one parameter, with type %s", context.entityTypeName());
        }

        for (RepositoryGenerator generator : generators) {
            addToSingle(
                    generator,
                    context,
                    MethodSpec.overriding(context.parametersInfo().element()));
        }
    }
}
