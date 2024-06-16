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
import javax.lang.model.element.TypeElement;
import org.geysermc.databaseutils.processor.query.QueryInfo;
import org.geysermc.databaseutils.processor.type.RepositoryGenerator;
import org.geysermc.databaseutils.processor.util.InvalidRepositoryException;
import org.geysermc.databaseutils.processor.util.TypeUtils;

final class DeleteAction extends Action {
    DeleteAction() {
        super("delete");
    }

    @Override
    protected boolean validateSingle(QueryInfo info, TypeElement returnType, TypeUtils typeUtils) {
        // todo does it also support saying how many items were deleted?
        if (!TypeUtils.isType(Void.class, returnType)) {
            throw new InvalidRepositoryException(
                    "Expected Void as return type for %s, got %s",
                    info.element().getSimpleName(), returnType);
        }
        return true;
    }

    @Override
    protected void addToSingle(
            RepositoryGenerator generator,
            QueryInfo info,
            MethodSpec.Builder spec,
            TypeElement returnType,
            boolean async) {
        generator.addDelete(info, spec, returnType, async);
    }
}
