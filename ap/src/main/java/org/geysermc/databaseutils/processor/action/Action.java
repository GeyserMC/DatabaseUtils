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
import javax.lang.model.element.TypeElement;
import org.geysermc.databaseutils.processor.query.QueryInfo;
import org.geysermc.databaseutils.processor.type.RepositoryGenerator;
import org.geysermc.databaseutils.processor.util.InvalidRepositoryException;
import org.geysermc.databaseutils.processor.util.TypeUtils;

public abstract class Action {
    private final String actionType;
    private final boolean supportsFilter;

    protected Action(String actionType) {
        this(actionType, true);
    }

    protected Action(String actionType, boolean supportsFilter) {
        this.actionType = actionType;
        this.supportsFilter = supportsFilter;
    }

    public String actionType() {
        return actionType;
    }

    public boolean supportsFilter() {
        return supportsFilter;
    }

    protected abstract void addToSingle(
            RepositoryGenerator generator,
            QueryInfo info,
            MethodSpec.Builder spec,
            TypeElement returnType,
            boolean async);

    protected boolean validateSingle(QueryInfo info, TypeElement returnType, TypeUtils typeUtils) {
        return TypeUtils.isType(Void.class, returnType) || TypeUtils.isWholeNumberType(returnType);
    }

    protected boolean validateCollection(QueryInfo info, TypeElement elementType, TypeUtils typeUtils) {
        return false;
    }

    protected boolean validateEither(QueryInfo info, TypeElement elementType, boolean collection, TypeUtils typeUtils) {
        return false;
    }

    protected void validate(QueryInfo info, TypeElement returnType, TypeUtils typeUtils) {
        var elementType = returnType;
        if (typeUtils.isAssignable(Collection.class, returnType.asType())) {
            elementType = (TypeElement) returnType.getTypeParameters().get(0);

            if (!supportsFilter) {
                throw new InvalidRepositoryException("%s does not support a By section", actionType);
            }

            if (validateCollection(info, elementType, typeUtils)
                    || validateEither(info, elementType, true, typeUtils)) {
                return;
            }
        } else {
            if (validateEither(info, elementType, false, typeUtils)) {
                return;
            }
            if (validateSingle(info, returnType, typeUtils)) {
                return;
            }
        }

        if (!typeUtils.isAssignable(info.entityType(), elementType.asType())) {
            throw new InvalidRepositoryException(
                    "Unsupported return type %s for %s",
                    returnType.getSimpleName(), info.element().getSimpleName());
        }
    }

    public void addTo(
            List<RepositoryGenerator> generators,
            QueryInfo info,
            TypeElement returnType,
            boolean async,
            TypeUtils typeUtils) {
        if (!info.hasBySection() && info.element().getParameters().size() != 1) {
            throw new InvalidRepositoryException("Expected one parameter with type %s", info.entityType());
        }
        validate(info, returnType, typeUtils);

        for (RepositoryGenerator generator : generators) {
            addToSingle(generator, info, MethodSpec.overriding(info.element()), returnType, async);
        }
    }
}
