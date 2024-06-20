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
import javax.lang.model.type.TypeMirror;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.query.QueryInfo;
import org.geysermc.databaseutils.processor.query.section.projection.ProjectionKeywordCategory;
import org.geysermc.databaseutils.processor.type.RepositoryGenerator;
import org.geysermc.databaseutils.processor.util.InvalidRepositoryException;
import org.geysermc.databaseutils.processor.util.TypeUtils;

final class FindAction extends Action {
    FindAction() {
        super(
                "find",
                true,
                true,
                ProjectionKeywordCategory.UNIQUE,
                ProjectionKeywordCategory.SUMMARY,
                ProjectionKeywordCategory.LIMIT);
    }

    @Override
    protected boolean validateEither(
            EntityInfo info, CharSequence methodName, TypeMirror returnType, boolean collection, TypeUtils typeUtils) {
        if (!typeUtils.isType(info.type().asType(), returnType)) {
            throw new InvalidRepositoryException(
                    "Expected %s as return type for %s, got %s", info.typeName(), methodName, returnType);
        }
        return true;
    }

    @Override
    protected boolean validateCollection(
            EntityInfo info, CharSequence methodName, TypeMirror returnType, TypeUtils typeUtils) {
        return false;
    }

    @Override
    public void addToSingle(RepositoryGenerator generator, QueryInfo info, MethodSpec.Builder spec) {
        generator.addFind(info, spec);
    }
}
