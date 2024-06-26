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
    private final List<QueryBuilderColumn> columns = new ArrayList<>();
    private int parameterIndex = 0;

    public QueryBuilder(QueryContext queryContext) {
        this.queryContext = queryContext;
    }

    public String query() {
        return query.toString();
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

    public QueryBuilder addRaw(String queryPart, String... format) {
        return addRaw(queryPart, List.of(format));
    }

    public QueryBuilder addRaw(String queryPart, List<String> format) {
        if (!query.isEmpty()) {
            query.append(' ');
        }
        query.append(queryPart.formatted(format.toArray()));
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

    public record QueryBuilderColumn(ColumnInfo info, CharSequence parameterName) {}
}
