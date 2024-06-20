/*
 * Copyright (c) 2024 GeyserMC <https://geysermc.org>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.query;

import com.google.auto.common.MoreTypes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
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
 * Analyses and validates the read Keywords and converts it into QueryInfo.
 * Note that this will edit the provided readResult. It doesn't create a new instance.
 */
public class QueryInfoCreator {
    private final Action action;
    private final KeywordsReadResult readResult;
    private final ExecutableElement element;
    private final EntityInfo info;
    private final TypeUtils typeUtils;

    private final TypeMirror returnType;
    private final boolean async;
    private final boolean isCollection;

    public QueryInfoCreator(
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
        this.isCollection = returnType != null && typeUtils.isAssignable(returnType, Collection.class);
    }

    public QueryInfo create() {
        analyseAndValidate();
        return new QueryInfo(
                info,
                readResult,
                new ParametersTypeInfo(element, info.typeName(), typeUtils),
                new ReturnTypeInfo(async, returnType, typeUtils),
                typeUtils);
    }

    private void analyseAndValidate() {
        var parameterTypes =
                element.getParameters().stream().map(VariableElement::asType).toList();
        var parameterNames = element.getParameters().stream()
                .map(VariableElement::getSimpleName)
                .toList();
        AtomicInteger handledInputs = new AtomicInteger();

        if (readResult.projection() != null) {
            validateColumnNames(readResult.projection().projections(), SectionType.PROJECTION, null);

            var handledCategories = new ArrayList<ProjectionKeywordCategory>();
            for (@NonNull ProjectionFactor projection : readResult.projection().projections()) {
                if (projection.keyword() == null) {
                    continue;
                }
                var category = projection.keyword().category();
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
        }

        if (readResult.bySection() != null) {
            validateColumnNames(
                    readResult.bySection().factors(), SectionType.BY, (VariableByFactor input, ColumnInfo column) -> {
                        var keyword = input.keyword();
                        keyword.validateTypes(column, parameterTypes, parameterNames, handledInputs.get(), typeUtils);
                        handledInputs.addAndGet(keyword.inputCount());
                    });
        }

        if (readResult.orderBySection() != null) {
            validateColumnNames(
                    readResult.orderBySection().factors(),
                    SectionType.ORDER_BY,
                    ($, $$) -> handledInputs.incrementAndGet());
        }

        // if there is no By section and there are parameters, it should be the entity or the provided projection
        if (readResult.bySection() == null && parameterTypes.size() == 1) {
            if (typeUtils.isAssignable(parameterTypes.get(0), Collection.class)
                    && !action.allowSelfCollectionArgument()) {
                throw new InvalidRepositoryException(
                        "Action %s (for %s) doesn't support return a collection!",
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
                    return;
                }
            }

            action.validate(info, element.getSimpleName(), returnType, typeUtils, null);
            return;
        }

        // Otherwise the expected parameter count should equal the actual
        if (parameterTypes.size() != handledInputs.get()) {
            throw new IllegalStateException(
                    "Expected %s parameters, received %s".formatted(handledInputs, parameterTypes));
        }
    }

    private <T extends VariableFactor> void validateColumnNames(
            List<? extends Factor> factors, SectionType type, BiConsumer<T, ColumnInfo> customValidation) {
        var variableLast = true;
        for (Factor factor : factors) {
            if (factor instanceof VariableFactor variable) {
                var column = info.columnFor(variable.columnName());
                if (column == null) {
                    throw new IllegalStateException(
                            "Could not find column %s for entity %s".formatted(variable.columnName(), info.name()));
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
            throw new IllegalStateException("Cannot end a section (%s) with a factor!".formatted(type));
        }
    }
}
