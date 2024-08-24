/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.query.section.by;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.lang.model.element.VariableElement;
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
            ColumnInfo column, List<? extends VariableElement> inputs, int offset, TypeUtils typeUtils) {
        if (offset + inputCount() > inputs.size()) {
            throw new IllegalStateException(
                    String.format("Expected (at least) %s inputs, got %s", offset + inputCount(), inputs.size()));
        }

        for (int i = 0; i < inputCount(); i++) {
            var input = inputs.get(offset + i);
            var type = input.asType();
            var name = input.getSimpleName();

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

    public boolean isIncomplete() {
        return parameterNames.size() != inputCount();
    }

    public InputKeyword addParameterName(@NonNull CharSequence parameterName) {
        parameterNames.add(parameterName);
        return this;
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
