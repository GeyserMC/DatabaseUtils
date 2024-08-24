/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.query;

import com.google.auto.common.MoreTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.databaseutils.processor.action.Action;
import org.geysermc.databaseutils.processor.info.ColumnInfo;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.query.section.SectionType;
import org.geysermc.databaseutils.processor.query.section.factor.Factor;
import org.geysermc.databaseutils.processor.query.section.factor.ProjectionFactor;
import org.geysermc.databaseutils.processor.query.section.factor.VariableByFactor;
import org.geysermc.databaseutils.processor.query.section.factor.VariableFactor;
import org.geysermc.databaseutils.processor.query.section.projection.ProjectionKeywordCategory;
import org.geysermc.databaseutils.processor.query.type.ParametersTypeInfo;
import org.geysermc.databaseutils.processor.query.type.ReturnTypeInfo;
import org.geysermc.databaseutils.processor.util.InvalidRepositoryException;
import org.geysermc.databaseutils.processor.util.TypeUtils;

/**
 * Analyses and validates the read Keywords and converts it into QueryContext.
 * Note that this will edit the provided readResult. It doesn't create a new instance.
 */
public class QueryContextCreator {
    private final Action action;
    private final KeywordsReadResult readResult;
    private final ExecutableElement element;
    private final EntityInfo info;
    private final TypeUtils typeUtils;

    private final TypeMirror returnType;
    private final boolean async;

    public QueryContextCreator(
            Action action,
            KeywordsReadResult readResult,
            ExecutableElement element,
            EntityInfo info,
            TypeUtils typeUtils) {
        this.action = action;
        this.readResult = readResult;
        this.element = element;
        this.info = info;
        this.typeUtils = typeUtils;

        TypeMirror returnType;
        boolean async = false;
        if (MoreTypes.isTypeOf(CompletableFuture.class, element.getReturnType())) {
            async = true;
            returnType = typeUtils.toBoxedMirror(
                    ((DeclaredType) element.getReturnType()).getTypeArguments().get(0));
        } else {
            returnType = typeUtils.toBoxedMirror(element.getReturnType());
        }

        this.returnType = returnType;
        this.async = async;
    }

    public QueryContext create() {
        var parameterInfo = analyseValidateAndCreate();
        return new QueryContext(
                info, readResult, parameterInfo, new ReturnTypeInfo(async, returnType, typeUtils), typeUtils);
    }

    private ParametersTypeInfo analyseValidateAndCreate() {
        var hasColumnParameter = readResult.projection() != null
                && readResult.projection().columnName() != null
                && action.projectionColumnIsParameter();
        var parameterInfo = new ParametersTypeInfo(element, info.type().asType(), typeUtils, hasColumnParameter);

        AtomicInteger handledInputs = new AtomicInteger();

        if (readResult.projection() != null) {
            validateColumnNames(readResult.projection().projections(), SectionType.PROJECTION, null);

            var handledCategories = new ArrayList<ProjectionKeywordCategory>();
            for (@NonNull ProjectionFactor projection : readResult.projection().projections()) {
                // ignore columnName factor
                if (projection.keyword() == null) {
                    continue;
                }

                var category = projection.keyword().category();
                if (!action.supportedProjectionCategories().contains(category)) {
                    if (action.supportedProjectionCategories().isEmpty()) {
                        throw new InvalidRepositoryException(
                                "Action %s doesn't support projection", action.actionType());
                    }
                    throw new InvalidRepositoryException(
                            "Action %s doesn't support projection category %s",
                            action.actionType(), category.toString());
                }
                if (!handledCategories.add(category)) {
                    throw new InvalidRepositoryException(
                            "You can only provide one keyword of category %s, also got %s",
                            category, projection.keyword().name());
                }
                if (category.requiresColumn() && readResult.projection().columnName() == null) {
                    throw new InvalidRepositoryException(
                            "Projection %s requires you to specify a column",
                            projection.keyword().name());
                }
            }

            if (action.projectionColumnIsParameter()) {
                handledInputs.incrementAndGet();
            }
        }

        if (readResult.bySection() != null) {
            validateColumnNames(
                    readResult.bySection().factors(), SectionType.BY, (VariableByFactor input, ColumnInfo column) -> {
                        var keyword = input.keyword();
                        keyword.validateTypes(column, element.getParameters(), handledInputs.get(), typeUtils);
                        handledInputs.addAndGet(keyword.inputCount());
                    });
        }

        if (readResult.orderBySection() != null) {
            validateColumnNames(
                    readResult.orderBySection().factors(),
                    SectionType.ORDER_BY,
                    ($, $$) -> handledInputs.incrementAndGet());
        }

        var parameterCount = element.getParameters().size();

        // if there is no By section and there are parameters, it should be the entity or the provided projection
        if (readResult.bySection() == null && parameterCount == 1) {
            if (parameterInfo.isAnySelf() && !action.allowSelfParameter()) {
                throw new InvalidRepositoryException(
                        "Action %s (for %s) doesn't support entity as parameter!",
                        action.actionType(), element.getSimpleName());
            }
            if (parameterInfo.isSelfCollection() && !action.allowReturnSelfCollection()) {
                throw new InvalidRepositoryException(
                        "Action %s (for %s) doesn't support returning an entity collection!",
                        action.actionType(), element.getSimpleName());
            }

            if (readResult.projection() != null) {
                var column = info.columnFor(readResult.projection().columnName());
                // specifying the columnName is optional
                if (column != null) {
                    action.validate(info, element.getSimpleName(), returnType, typeUtils, type -> {
                        if (!typeUtils.isAssignable(type, column.typeName())) {
                            throw new InvalidRepositoryException(
                                    "Expected response of %s to be assignable from %s",
                                    element.getSimpleName(), column.typeName());
                        }
                    });
                    return parameterInfo;
                }
            }

            action.validate(info, element.getSimpleName(), returnType, typeUtils, null);
            return parameterInfo;
        }

        // Otherwise the expected parameter count should equal the actual
        if (parameterCount != handledInputs.get()) {
            throw new InvalidRepositoryException("Expected %s parameters, received %s", handledInputs, parameterCount);
        }
        return parameterInfo;
    }

    private <T extends VariableFactor> void validateColumnNames(
            List<? extends Factor> factors, SectionType type, BiConsumer<T, ColumnInfo> customValidation) {
        var variableLast = true;
        for (Factor factor : factors) {
            if (factor instanceof VariableFactor variable) {
                var column = info.columnFor(variable.columnName());
                if (column == null) {
                    throw new InvalidRepositoryException(
                            "Could not find column %s for entity %s", variable.columnName(), info.name());
                }
                variableLast = true;
                if (customValidation != null) {
                    //noinspection unchecked
                    customValidation.accept((T) variable, column);
                }
            } else {
                variableLast = false;
            }
        }

        if (!variableLast) {
            throw new InvalidRepositoryException("Cannot end a section (%s) with a factor!", type);
        }
    }
}
