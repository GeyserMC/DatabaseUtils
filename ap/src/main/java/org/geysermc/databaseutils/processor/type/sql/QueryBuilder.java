/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.type.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.databaseutils.processor.info.ColumnInfo;
import org.geysermc.databaseutils.processor.query.QueryContext;
import org.geysermc.databaseutils.processor.query.section.factor.VariableByFactor;

public class QueryBuilder {
    private final QueryContext queryContext;
    private final StringBuilder query = new StringBuilder();
    private final StringBuilder endQuery = new StringBuilder();
    private final List<QueryBuilderColumn> columns = new ArrayList<>();
    private int parameterIndex = 0;

    public QueryBuilder(QueryContext queryContext) {
        this.queryContext = queryContext;
    }

    public String query() {
        if (!query.isEmpty() && !endQuery.isEmpty()) {
            return query + " " + endQuery;
        }
        return query + endQuery.toString();
    }

    public List<QueryBuilderColumn> columns() {
        return columns;
    }

    public QueryBuilder add(String queryPart, BiFunction<QueryContext, QueryBuilder, String> function) {
        return addRaw(queryPart, function.apply(queryContext, this));
    }

    public QueryBuilder add(String queryPart, boolean parameters, String... columnNames) {
        return add(queryPart, parameters, List.of(columnNames));
    }

    public QueryBuilder add(String queryPart, boolean parameters, List<String> columnNames) {
        addRaw(queryPart, columnNames);
        for (String columnName : columnNames) {
            addColumn(columnName, parameters);
        }
        return this;
    }

    public QueryBuilder addRaw(String queryPart, List<String> format) {
        return addRaw(queryPart, format.toArray(String[]::new));
    }

    public QueryBuilder addRaw(String queryPart, String... format) {
        if (!query.isEmpty()) {
            query.append(' ');
        }
        query.append(queryPart.formatted((Object[]) format));
        return this;
    }

    public QueryBuilder addRawBefore(String before, String queryPart, String... format) {
        var index = query.indexOf(before);
        if (index == -1) {
            throw new IllegalArgumentException(String.format(
                    "Cannot add '%s' before '%s' as before is not present in '%s'", queryPart, before, query));
        }
        query.insert(index, queryPart.formatted((Object[]) format) + " ");
        return this;
    }

    /**
     * Optionally add before, if before is not found then {@link #addRaw(String, String...)} is called
     */
    public QueryBuilder addRawOptionalBefore(String before, String queryPart, String... format) {
        int index = query.indexOf(before);
        if (index == -1) {
            return addRaw(queryPart, format);
        }
        query.insert(index, queryPart.formatted((Object[]) format) + (query.isEmpty() ? "" : " "));
        return this;
    }

    public QueryBuilder addEndRaw(String queryPart, String... format) {
        if (!endQuery.isEmpty()) {
            endQuery.append(' ');
        }
        endQuery.append(queryPart.formatted((Object[]) format));
        return this;
    }

    public QueryBuilder addEndStartRaw(String queryPart, String... format) {
        if (!endQuery.isEmpty()) {
            endQuery.insert(0, ' ');
        }
        endQuery.insert(0, queryPart.formatted((Object[]) format));
        return this;
    }

    public QueryBuilder addColumn(CharSequence columnName, boolean parameter) {
        var parameterName = parameter ? queryContext.parametersInfo().name(parameterIndex++) : null;
        columns.add(new QueryBuilderColumn(queryContext.columnFor(columnName), parameterName));
        return this;
    }

    public QueryBuilder addColumn(ColumnInfo info) {
        columns.add(new QueryBuilderColumn(info, null));
        return this;
    }

    public QueryBuilder addColumn(VariableByFactor variable, boolean parameter) {
        if (variable.keyword().isIncomplete()) {
            addColumn(variable.columnName(), parameter);
            return this;
        }

        for (@NonNull CharSequence parameterName : variable.keyword().parameterNames()) {
            columns.add(new QueryBuilderColumn(queryContext.columnFor(variable.columnName()), parameterName));
        }
        return this;
    }

    public QueryBuilder addAll(List<ColumnInfo> columns) {
        columns.forEach(this::addColumn);
        return this;
    }

    public QueryBuilder copy() {
        var builder = new QueryBuilder(queryContext);
        builder.query.append(query);
        builder.endQuery.append(endQuery);
        builder.columns.addAll(columns);
        builder.parameterIndex = parameterIndex;
        return builder;
    }

    @Override
    public String toString() {
        return query();
    }

    public record QueryBuilderColumn(ColumnInfo info, CharSequence parameterName) {}
}
