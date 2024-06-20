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
package org.geysermc.databaseutils.processor.action;

import com.squareup.javapoet.MethodSpec;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.query.QueryInfo;
import org.geysermc.databaseutils.processor.query.section.projection.ProjectionKeywordCategory;
import org.geysermc.databaseutils.processor.type.RepositoryGenerator;
import org.geysermc.databaseutils.processor.util.InvalidRepositoryException;
import org.geysermc.databaseutils.processor.util.TypeUtils;

public abstract class Action {
    private final String actionType;
    private final boolean allowSelfCollectionArgument;
    private final boolean supportsFilter;
    private final List<ProjectionKeywordCategory> supportedProjectionCategories;

    protected Action(
            String actionType,
            boolean allowSelfCollectionArgument,
            boolean supportsFilter,
            ProjectionKeywordCategory... supportedProjectionCategories) {
        this.actionType = actionType;
        this.allowSelfCollectionArgument = allowSelfCollectionArgument;
        this.supportsFilter = supportsFilter;
        this.supportedProjectionCategories = List.of(supportedProjectionCategories);
    }

    public String actionType() {
        return actionType;
    }

    public boolean allowSelfCollectionArgument() {
        return allowSelfCollectionArgument;
    }

    public boolean supportsFilter() {
        return supportsFilter;
    }

    public List<ProjectionKeywordCategory> unsupportedProjectionCategories() {
        return supportedProjectionCategories;
    }

    protected abstract void addToSingle(RepositoryGenerator generator, QueryInfo info, MethodSpec.Builder spec);

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

    public void addTo(List<RepositoryGenerator> generators, QueryInfo info) {
        if (!info.hasBySection() && !info.parametersInfo().isNoneOrAnySelf()) {
            throw new InvalidRepositoryException("Expected at most one parameter, with type %s", info.entityType());
        }

        for (RepositoryGenerator generator : generators) {
            addToSingle(
                    generator, info, MethodSpec.overriding(info.parametersInfo().element()));
        }
    }
}
