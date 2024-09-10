/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.action;

import com.squareup.javapoet.MethodSpec;
import java.util.List;
import java.util.function.Consumer;
import javax.lang.model.type.TypeMirror;
import org.geysermc.databaseutils.processor.query.QueryContext;
import org.geysermc.databaseutils.processor.query.section.projection.ProjectionKeywordCategory;
import org.geysermc.databaseutils.processor.type.RepositoryGenerator;
import org.geysermc.databaseutils.processor.util.InvalidRepositoryException;

public abstract class Action {
    private final String actionType;
    private final boolean allowSelfParameter;
    private final boolean allowReturnAnySelfOrColumn;
    private final boolean supportsFilter;
    private final boolean remainingParametersAsColumns;
    private final List<ProjectionKeywordCategory> supportedProjectionCategories;

    protected Action(
            String actionType,
            boolean allowSelfParameter,
            boolean allowReturnAnySelfOrColumn,
            boolean supportsFilter,
            boolean remainingParametersAsColumns,
            ProjectionKeywordCategory... supportedProjectionCategories) {
        this.actionType = actionType;
        this.allowSelfParameter = allowSelfParameter;
        this.allowReturnAnySelfOrColumn = allowReturnAnySelfOrColumn;
        this.supportsFilter = supportsFilter;
        this.remainingParametersAsColumns = remainingParametersAsColumns;
        this.supportedProjectionCategories = List.of(supportedProjectionCategories);
    }

    public String actionType() {
        return actionType;
    }

    public boolean allowSelfParameter() {
        return allowSelfParameter;
    }

    public boolean allowReturnAnySelfOrColumn() {
        return allowReturnAnySelfOrColumn;
    }

    public boolean supportsFilter() {
        return supportsFilter;
    }

    public boolean remainingParametersAsColumns() {
        return remainingParametersAsColumns;
    }

    public List<ProjectionKeywordCategory> supportedProjectionCategories() {
        return supportedProjectionCategories;
    }

    protected abstract void addToSingle(RepositoryGenerator generator, QueryContext context, MethodSpec.Builder spec);

    protected boolean validateSingle(QueryContext context, boolean passedCustomValidation) {
        return context.typeUtils().isType(Void.class, context.returnType())
                || context.typeUtils().isWholeNumberType(context.returnType());
    }

    protected boolean validateCollection(QueryContext context, boolean passedCustomValidation) {
        throw new InvalidRepositoryException(
                "Collection return type (%s) is not supported for %s", context.returnType(), actionType());
    }

    protected boolean validateEither(QueryContext context, boolean passedCustomValidation) {
        return false;
    }

    public void validate(QueryContext context, Consumer<TypeMirror> customValidation) {
        var customValidationOk = false;
        if (context.returnInfo().isCollection()) {
            if (!supportsFilter) {
                throw new InvalidRepositoryException("%s does not support a By section", actionType);
            }

            if (customValidation != null) {
                customValidation.accept(context.returnInfo().elementTypeOrType());
                customValidationOk = true;
            }

            if (validateCollection(context, customValidationOk) || validateEither(context, customValidationOk)) {
                return;
            }
        } else {
            if (customValidation != null) {
                customValidation.accept(context.returnInfo().elementTypeOrType());
                customValidationOk = true;
            }

            if (validateEither(context, customValidationOk)) {
                return;
            }
            if (validateSingle(context, customValidationOk)) {
                return;
            }
        }

        if (!customValidationOk) {
            throw new InvalidRepositoryException(
                    "Unsupported return type %s for %s", context.returnType(), context.methodName());
        }
    }

    public void addTo(List<RepositoryGenerator> generators, QueryContext context) {
        // todo is this condition needed here, after the action validation?
        if (!context.hasBySection()
                && !context.parametersInfo().isNoneOrAnySelf()
                && !context.hasProjectionColumnName()) {
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
