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
package org.geysermc.databaseutils.processor.query.section.by;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.databaseutils.processor.info.ColumnInfo;
import org.geysermc.databaseutils.processor.util.CollectionUtils;
import org.geysermc.databaseutils.processor.util.TypeUtils;

/**
 * A keyword that requires multiple inputs from the user.
 */
public abstract class InputKeyword {
    private final List<CharSequence> parameterNames = new ArrayList<>();

    public abstract @NonNull List<@NonNull String> names();

    /**
     * Returns for each input it's supported types
     */
    public abstract List<List<Class<?>>> acceptedInputs();

    public int inputCount() {
        return acceptedInputs().size();
    }

    public void validateTypes(
            ColumnInfo column,
            List<TypeMirror> inputTypes,
            List<? extends CharSequence> inputNames,
            int typeOffset,
            TypeUtils typeUtils) {

        if (typeOffset + inputCount() > inputTypes.size()) {
            throw new IllegalStateException(String.format(
                    "Expected (at least) %s inputs, got %s", typeOffset + inputCount(), inputTypes.size()));
        }

        for (int i = 0; i < inputCount(); i++) {
            var type = inputTypes.get(typeOffset + i);
            var name = inputNames.get(typeOffset + i);

            if (!typeUtils.isAssignable(column.typeName(), type)) {
                throw new IllegalStateException(String.format(
                        "Expected a type assignable from column %s as %s with type %s, got %s",
                        column.name(), name, column.typeName(), typeUtils.canonicalName(type)));
            }

            var acceptedTypes = acceptedInputs().get(i);
            if (acceptedTypes.stream().noneMatch(clazz -> typeUtils.isAssignable(type, clazz))) {
                throw new IllegalStateException(String.format(
                        "Unsupported type provided for parameter %s of %s. Received %s, expected %s",
                        name,
                        getClass().getName(),
                        typeUtils.canonicalName(type),
                        CollectionUtils.join(acceptedTypes)));
            }

            addParameterName(name);
        }
    }

    public @NonNull List<@NonNull CharSequence> parameterNames() {
        return parameterNames;
    }

    public void addParameterName(@NonNull CharSequence parameterName) {
        parameterNames.add(parameterName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InputKeyword that = (InputKeyword) o;
        return Objects.equals(parameterNames, that.parameterNames);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(parameterNames);
    }
}
