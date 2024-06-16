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

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.query.section.factor.Factor;
import org.geysermc.databaseutils.processor.query.section.factor.VariableByFactor;
import org.geysermc.databaseutils.processor.query.section.factor.VariableOrderByFactor;
import org.geysermc.databaseutils.processor.util.TypeUtils;

/**
 * Analyses and validates the read Keywords and converts it into QueryInfo.
 * Note that this will edit the provided readResult. It doesn't create a new instance.
 */
public class QueryInfoCreator {
    private final KeywordsReadResult readResult;
    private final ExecutableElement element;
    private final EntityInfo info;
    private final TypeUtils typeUtils;

    public QueryInfoCreator(
            KeywordsReadResult readResult, ExecutableElement element, EntityInfo info, TypeUtils typeUtils) {
        this.readResult = readResult;
        this.element = element;
        this.info = info;
        this.typeUtils = typeUtils;
    }

    public QueryInfo create() {
        analyse();
        return new QueryInfo(info, readResult, element);
    }

    private void analyse() {
        var parameterTypes =
                element.getParameters().stream().map(VariableElement::asType).toList();
        var parameterNames = element.getParameters().stream()
                .map(VariableElement::getSimpleName)
                .toList();
        var handledInputs = 0;

        if (readResult.bySection() != null) {
            var section = readResult.bySection();
            for (Factor factor : section.factors()) {
                if (factor instanceof VariableByFactor variable) {
                    var column = info.columnFor(variable.name());
                    if (column == null) {
                        throw new IllegalStateException(
                                "Could not find column %s for entity %s".formatted(variable.name(), info.name()));
                    }

                    var keyword = variable.keyword();
                    keyword.validateTypes(column, parameterTypes, parameterNames, handledInputs, typeUtils);
                    handledInputs += keyword.inputCount();
                }
            }
        }

        if (readResult.orderBySection() != null) {
            var section = readResult.orderBySection();
            for (Factor factor : section.factors()) {
                if (factor instanceof VariableOrderByFactor variable) {
                    var column = info.columnFor(variable.name());
                    if (column == null) {
                        throw new IllegalStateException(
                                "Could not find column %s for entity %s".formatted(variable.name(), info.name()));
                    }
                }
            }
        }

        // if there is no By section and there are parameters, it should be the entity
        if (readResult.bySection() == null && parameterTypes.size() == 1) {
            if (!typeUtils.isAssignable(parameterTypes.get(0), info.className())) {
                throw new IllegalStateException(String.format(
                        "Expected the only parameter %s to be assignable from entity %s with type %s!",
                        parameterTypes.get(0), info.name(), info.className()));
            }
            return;
        }

        // Otherwise the expected parameter count should equal the actual
        if (parameterTypes.size() != handledInputs) {
            throw new IllegalStateException(
                    "Expected %s parameters, received %s".formatted(handledInputs, parameterTypes));
        }
    }
}
