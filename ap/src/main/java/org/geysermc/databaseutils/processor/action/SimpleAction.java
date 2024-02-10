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
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Types;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.type.RepositoryGenerator;
import org.geysermc.databaseutils.processor.util.InvalidRepositoryException;
import org.geysermc.databaseutils.processor.util.TypeUtils;

abstract class SimpleAction extends Action {
    protected SimpleAction(String actionType) {
        super(actionType, '^' + actionType + '$');
    }

    protected abstract void addToSingle(
            RepositoryGenerator generator,
            EntityInfo info,
            VariableElement parameter,
            MethodSpec.Builder spec,
            boolean async);

    @Override
    public void addTo(
            List<RepositoryGenerator> generators,
            String fullName,
            ExecutableElement element,
            TypeElement returnType,
            EntityInfo info,
            Types typeUtils,
            boolean async) {
        if (!TypeUtils.isTypeOf(Void.class, returnType)) {
            throw new InvalidRepositoryException(
                    "Expected Void as return type for %s, got %s", element.getSimpleName(), returnType);
        }
        if (element.getParameters().size() == 1) {
            var parameter = element.getParameters().get(0);
            if (TypeUtils.isTypeOf(info.className(), parameter.asType())) {
                for (RepositoryGenerator generator : generators) {
                    addToSingle(generator, info, parameter, MethodSpec.overriding(element), async);
                }
                return;
            }
        }
        throw new InvalidRepositoryException("Expected one parameter with type %s", info.className());
    }
}
