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
package org.geysermc.databaseutils.processor.type;

import static org.geysermc.databaseutils.processor.type.sql.JdbcTypeMappingRegistry.jdbcGetFor;
import static org.geysermc.databaseutils.processor.type.sql.JdbcTypeMappingRegistry.jdbcSetFor;
import static org.geysermc.databaseutils.processor.util.StringUtils.repeat;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import javax.lang.model.element.Modifier;
import org.geysermc.databaseutils.processor.info.ColumnInfo;
import org.geysermc.databaseutils.processor.query.QueryContext;
import org.geysermc.databaseutils.processor.query.section.by.keyword.EqualsKeyword;
import org.geysermc.databaseutils.processor.query.section.by.keyword.LessThanKeyword;
import org.geysermc.databaseutils.processor.query.section.factor.AndFactor;
import org.geysermc.databaseutils.processor.query.section.factor.Factor;
import org.geysermc.databaseutils.processor.query.section.factor.OrFactor;
import org.geysermc.databaseutils.processor.query.section.factor.VariableByFactor;
import org.geysermc.databaseutils.processor.query.section.projection.ProjectionKeyword;
import org.geysermc.databaseutils.processor.query.section.projection.keyword.AvgProjectionKeyword;
import org.geysermc.databaseutils.processor.query.section.projection.keyword.TopProjectionKeyword;
import org.geysermc.databaseutils.processor.type.sql.QueryBuilder;
import org.geysermc.databaseutils.processor.type.sql.QueryBuilder.QueryBuilderColumn;
import org.geysermc.databaseutils.processor.util.InvalidRepositoryException;
import org.geysermc.databaseutils.processor.util.TypeUtils;

public class SqlRepositoryGenerator extends RepositoryGenerator {
    private static final int BATCH_SIZE = 250;

    @Override
    protected String upperCamelCaseDatabaseType() {
        return "Sql";
    }

    @Override
    protected void onConstructorBuilder(MethodSpec.Builder builder) {
        typeSpec.addField(HikariDataSource.class, "dataSource", Modifier.PRIVATE, Modifier.FINAL);
        builder.addStatement("this.dataSource = database.dataSource()");
    }

    @Override
    public void addFind(QueryContext context, MethodSpec.Builder spec) {
        var builder = new QueryBuilder(context)
                .addRaw("select %s from %s", createProjectionFor(context), context.tableName());
        if (context.hasBySection()) {
            builder.add("where %s", this::createWhereForFactors);
        }

        addExecuteQueryData(spec, context, builder, () -> {
            if (context.returnInfo().isCollection()) {
                spec.addStatement(
                        "$T __responses = new $L<>()",
                        context.returnType(),
                        context.typeUtils().collectionImplementationFor(context.returnType()));
                spec.beginControlFlow("while (__result.next())");
            } else {
                spec.beginControlFlow("if (!__result.next())");
                spec.addStatement("return null");
                spec.endControlFlow();
            }

            var arguments = new ArrayList<String>();
            for (ColumnInfo column : context.columns()) {
                var columnType = ClassName.bestGuess(column.typeName().toString());

                var getFormat = jdbcGetFor(column.typeName(), "__result.%s(%s)");
                if (TypeUtils.needsTypeCodec(column.typeName())) {
                    getFormat = CodeBlock.of("this.__$L.decode($L)", column.name(), getFormat)
                            .toString();
                }

                spec.addStatement("$T _$L = %s".formatted(getFormat), columnType, column.name(), column.name());
                arguments.add("_" + column.name());
            }

            if (context.returnInfo().isCollection()) {
                spec.addStatement(
                        "__responses.add(new $T($L))",
                        ClassName.bestGuess(context.entityType().toString()),
                        String.join(", ", arguments));
                spec.endControlFlow();
                spec.addStatement("return __responses");
            } else {
                spec.addStatement(
                        "return new $T($L)",
                        ClassName.bestGuess(context.entityType().toString()),
                        String.join(", ", arguments));
            }
        });
    }

    @Override
    public void addExists(QueryContext context, MethodSpec.Builder spec) {
        var builder = new QueryBuilder(context).addRaw("select 1 from %s", context.tableName());
        if (context.hasBySection()) {
            builder.add("where %s", this::createWhereForFactors);
        }
        addExecuteQueryData(spec, context, builder, () -> spec.addStatement("return __result.next()"));
    }

    @Override
    public void addInsert(QueryContext context, MethodSpec.Builder spec) {
        var columnNames = String.join(
                ",", context.columns().stream().map(ColumnInfo::name).toList());
        var columnParameters = String.join(",", repeat("?", context.columns().size()));

        var builder = new QueryBuilder(context)
                .addRaw("insert into %s (%s) values (%s)", context.tableName(), columnNames, columnParameters)
                .addAll(context.columns());
        addUpdateQueryData(spec, context, builder);
    }

    @Override
    public void addUpdate(QueryContext context, MethodSpec.Builder spec) {
        var builder = new QueryBuilder(context)
                .addRaw("update %s", context.tableName())
                .add("set %s", this::createSetFor);

        if (context.hasBySection()) {
            builder.add("where %s", this::createWhereForFactors);
        } else {
            builder.add("where %s", this::createWhereForKeys);
        }
        addUpdateQueryData(spec, context, builder);
    }

    @Override
    public void addDelete(QueryContext context, MethodSpec.Builder spec) {
        var builder = new QueryBuilder(context).addRaw("delete from %s", context.tableName());
        if (context.hasBySection()) {
            builder.add("where %s", this::createWhereForFactors);
        } else {
            builder.add("where %s", this::createWhereForKeys);
        }
        addUpdateQueryData(spec, context, builder);
    }

    private void addExecuteQueryData(
            MethodSpec.Builder spec, QueryContext context, QueryBuilder builder, Runnable content) {
        addBySectionData(spec, context, builder, () -> {
            spec.beginControlFlow("try ($T __result = __statement.executeQuery())", ResultSet.class);
            content.run();
            spec.endControlFlow();
        });
    }

    private void addUpdateQueryData(MethodSpec.Builder spec, QueryContext context, QueryBuilder builder) {
        addBySectionData(spec, context, builder, () -> {
            if (!context.parametersInfo().isSelfCollection()) {
                spec.addStatement("__statement.executeUpdate()");
            }

            if (context.typeUtils().isType(Void.class, context.returnType())) {
                spec.addStatement("return $L", context.returnInfo().async() ? "null" : "");
            } else if (context.typeUtils().isType(context.entityType(), context.returnType())) {
                // todo support also creating an entity type from the given parameters
                spec.addStatement("return $L", context.parametersInfo().name(0));
            } else {
                throw new InvalidRepositoryException(
                        "Return type can be either void or %s but got %s", context.entityType(), context.returnType());
            }
        });
    }

    private void addBySectionData(
            MethodSpec.Builder spec, QueryContext context, QueryBuilder builder, Runnable execute) {

        wrapInCompletableFuture(spec, context.returnInfo().async(), () -> {
            spec.beginControlFlow("try ($T __connection = this.dataSource.getConnection())", Connection.class);
            spec.beginControlFlow(
                    "try ($T __statement = __connection.prepareStatement($S))",
                    PreparedStatement.class,
                    builder.query());

            CharSequence parameterName = "";
            if (context.parametersInfo().hasParameters()) {
                parameterName = context.parametersInfo().name(0);
            }

            if (context.parametersInfo().isSelfCollection()) {
                spec.addStatement("int __count = 0");
                spec.beginControlFlow("for (var __element : $L)", parameterName);
                parameterName = "__element";
            }

            int variableIndex = 0;
            for (QueryBuilderColumn column : builder.columns()) {
                var columnInfo = column.info();

                CharSequence input = "%s.%s()".formatted(parameterName, columnInfo.name());
                if (column.parameterName() != null) {
                    input = column.parameterName();
                }

                if (TypeUtils.needsTypeCodec(columnInfo.typeName())) {
                    input = CodeBlock.of("this.__$L.encode($L)", columnInfo.name(), input)
                            .toString();
                }
                // jdbc index starts at 1
                spec.addStatement(jdbcSetFor(columnInfo.typeName(), "__statement.%s($L, $L)"), ++variableIndex, input);
            }

            if (context.parametersInfo().isSelfCollection()) {
                spec.addStatement("__statement.addBatch()");

                spec.beginControlFlow("if (__count % $L == 0)", BATCH_SIZE);
                spec.addStatement("__statement.executeBatch()");
                spec.endControlFlow();

                spec.endControlFlow();
                spec.addStatement("__statement.executeBatch()");
                spec.addStatement("__connection.commit()");
            }

            execute.run();

            if (context.parametersInfo().isSelfCollection()) {
                spec.nextControlFlow("catch ($T __exception)", SQLException.class);
                spec.addStatement("__connection.rollback()");
                spec.addStatement("throw __exception");
            }
            spec.endControlFlow();

            spec.nextControlFlow("catch ($T __exception)", SQLException.class);
            spec.addStatement("throw new $T($S, __exception)", CompletionException.class, "Unexpected error occurred");
            spec.endControlFlow();
        });
        typeSpec.addMethod(spec.build());
    }

    private String createSetFor(QueryContext context, QueryBuilder builder) {
        if (context.projection() != null && context.projection().columnName() != null) {
            //noinspection DataFlowIssue
            var columns = List.of(context.columnFor(context.projection().columnName()));
            return createParametersForColumns(columns, null, ',', builder, true);
        }
        var columns = context.entityInfo().notKeyColumns();
        return createParametersForColumns(columns, null, ',', builder, false);
    }

    private String createWhereForKeys(QueryContext context, QueryBuilder builder) {
        return createParametersForColumns(
                context.entityInfo().keyColumns(), () -> AndFactor.INSTANCE, ' ', builder, false);
    }

    private String createWhereForFactors(QueryContext context, QueryBuilder builder) {
        return createParametersForFactors(context.bySectionFactors(), ' ', builder, true);
    }

    private String createParametersForColumns(
            List<ColumnInfo> columns,
            Supplier<Factor> factorSeparator,
            char separator,
            QueryBuilder builder,
            boolean parameter) {
        var factors = new ArrayList<Factor>();
        for (ColumnInfo column : columns) {
            if (!factors.isEmpty() && factorSeparator != null) {
                factors.add(factorSeparator.get());
            }
            factors.add(new VariableByFactor(column.name()));
        }
        return createParametersForFactors(factors, separator, builder, parameter);
    }

    private String createParametersForFactors(
            List<Factor> factors, char separator, QueryBuilder queryBuilder, boolean parameter) {
        var builder = new StringBuilder();
        for (Factor factor : factors) {
            if (!builder.isEmpty()) {
                builder.append(separator);
            }

            if (factor instanceof AndFactor) {
                builder.append("and");
                continue;
            }
            if (factor instanceof OrFactor) {
                builder.append("or");
                continue;
            }
            if (!(factor instanceof VariableByFactor variable)) {
                throw new InvalidRepositoryException(
                        "Unknown factor type %s", factor.getClass().getCanonicalName());
            }
            var keyword = variable.keyword();

            builder.append(variable.columnName());
            if (keyword instanceof EqualsKeyword) {
                builder.append("=?");
            } else if (keyword instanceof LessThanKeyword) {
                builder.append("<?");
            } else {
                throw new InvalidRepositoryException("Unsupported keyword %s", keyword);
            }

            queryBuilder.addColumn(variable, parameter);
        }
        return builder.toString();
    }

    private String createProjectionFor(QueryContext context) {
        var section = context.result().projection();
        if (section == null) {
            return "*";
        }
        var distinct = section.distinct();
        var columnName = section.columnName();

        var result = columnName != null ? columnName : "*";
        if (distinct != null) {
            result = "distinct " + result;
        }
        String limit = null;

        for (ProjectionKeyword projection : section.notDistinctProjectionKeywords()) {
            if (projection instanceof AvgProjectionKeyword) {
                if (!context.typeUtils().isWholeNumberType(context.returnType())) {
                    result = "avg(%s)".formatted(result);
                }
                continue;
            }
            if (projection instanceof TopProjectionKeyword keyword) {
                limit = "limit " + keyword.limit();
                continue;
            }
            throw new InvalidRepositoryException("Unsupported projection %s", projection.name());
        }

        return result + (limit != null ? ' ' + limit : "");
    }
}
