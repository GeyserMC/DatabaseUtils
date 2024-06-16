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
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.databaseutils.processor.info.ColumnInfo;
import org.geysermc.databaseutils.processor.query.QueryInfo;
import org.geysermc.databaseutils.processor.query.section.by.keyword.EqualsKeyword;
import org.geysermc.databaseutils.processor.query.section.by.keyword.LessThanKeyword;
import org.geysermc.databaseutils.processor.query.section.factor.AndFactor;
import org.geysermc.databaseutils.processor.query.section.factor.Factor;
import org.geysermc.databaseutils.processor.query.section.factor.OrFactor;
import org.geysermc.databaseutils.processor.query.section.factor.VariableByFactor;
import org.geysermc.databaseutils.processor.util.InvalidRepositoryException;
import org.geysermc.databaseutils.processor.util.TypeUtils;

public class SqlRepositoryGenerator extends RepositoryGenerator {
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
    public void addFind(QueryInfo info, MethodSpec.Builder spec, TypeElement returnType, boolean async) {
        // todo make it work without a By section
        var query = "select * from %s where %s".formatted(info.tableName(), createWhereForAll(info));
        addExecuteQueryData(spec, async, query, info, () -> {
            spec.beginControlFlow("if (!result.next())");
            spec.addStatement("return null");
            spec.endControlFlow();

            var arguments = new ArrayList<String>();
            for (ColumnInfo column : info.columns()) {
                var columnType = ClassName.bestGuess(column.typeName().toString());

                var getFormat = jdbcGetFor(column.typeName(), "result.%s(%s)");
                if (TypeUtils.needsTypeCodec(column.typeName())) {
                    getFormat = CodeBlock.of("this.__$L.decode($L)", column.name(), getFormat)
                            .toString();
                }

                spec.addStatement("$T _$L = %s".formatted(getFormat), columnType, column.name(), column.name());
                arguments.add("_" + column.name());
            }
            spec.addStatement(
                    "return new $T($L)",
                    ClassName.bestGuess(info.entityType().toString()),
                    String.join(", ", arguments));
        });
    }

    @Override
    public void addExists(QueryInfo info, MethodSpec.Builder spec, TypeElement returnType, boolean async) {
        // todo make it work without a By section
        var query = "select 1 from %s where %s".formatted(info.tableName(), createWhereForAll(info));
        addExecuteQueryData(spec, async, query, info, () -> spec.addStatement("return result.next()"));
    }

    @Override
    public void addInsert(QueryInfo info, MethodSpec.Builder spec, TypeElement returnType, boolean async) {
        var columnNames =
                String.join(",", info.columns().stream().map(ColumnInfo::name).toList());
        var columnParameters = String.join(",", repeat("?", info.columns().size()));
        var query = "insert into %s (%s) values (%s)".formatted(info.tableName(), columnNames, columnParameters);
        addUpdateQueryData(spec, returnType, async, query, info, info.columns());
    }

    @Override
    public void addUpdate(QueryInfo info, MethodSpec.Builder spec, TypeElement returnType, boolean async) {
        // todo make it work with By section
        var query =
                "update %s set %s where %s".formatted(info.tableName(), createSetFor(info), createWhereForKeys(info));
        addUpdateQueryData(
                spec, returnType, async, query, info, info.entityInfo().notKeyFirstColumns());
    }

    @Override
    public void addDelete(QueryInfo info, MethodSpec.Builder spec, TypeElement returnType, boolean async) {
        if (info.hasBySection()) {
            var query = "delete from %s where %s".formatted(info.tableName(), createWhereForAll(info));
            addUpdateQueryData(spec, Void.class.getCanonicalName(), async, query, info);
            return;
        }

        var query = "delete from %s where %s".formatted(info.tableName(), createWhereForKeys(info));
        addUpdateQueryData(
                spec, returnType, async, query, info, info.entityInfo().keyColumns());
    }

    private void addExecuteQueryData(
            MethodSpec.Builder spec, boolean async, String query, QueryInfo info, Runnable content) {
        addBySectionData(spec, async, query, info, () -> {
            spec.beginControlFlow("try ($T result = statement.executeQuery())", ResultSet.class);
            content.run();
            spec.endControlFlow();
        });
    }

    private void addUpdateQueryData(
            MethodSpec.Builder spec,
            TypeElement returnType,
            boolean async,
            String query,
            QueryInfo info,
            List<ColumnInfo> columns) {
        addNoBySectionData(spec, async, query, info, columns, () -> {
            spec.addStatement("statement.executeUpdate()");
            if (TypeUtils.isType(Void.class, returnType)) {
                spec.addStatement("return null");
            } else if (TypeUtils.isType(info.entityType(), returnType)) {
                // todo support also creating an entity type from the given parameters
                spec.addStatement("return $L", info.parameterName(0));
            } else {
                throw new InvalidRepositoryException(
                        "Return type can be either void or %s but got %s",
                        info.entityType(), returnType.getQualifiedName());
            }
        });
    }

    private void addUpdateQueryData(
            MethodSpec.Builder spec, CharSequence returnType, boolean async, String query, QueryInfo info) {
        addBySectionData(spec, async, query, info, () -> {
            spec.addStatement("statement.executeUpdate()");
            if (TypeUtils.isType(Void.class, returnType)) {
                spec.addStatement("return null");
            } else if (TypeUtils.isType(info.entityType(), returnType)) {
                // todo support also creating an entity type from the given parameters
                spec.addStatement("return $L", info.parameterName(0));
            } else {
                throw new InvalidRepositoryException(
                        "Return type can be either void or %s but got %s", info.entityType(), returnType);
            }
        });
    }

    private void addNoBySectionData(
            MethodSpec.Builder spec,
            boolean async,
            String query,
            QueryInfo info,
            List<ColumnInfo> columns,
            Runnable content) {
        wrapInCompletableFuture(spec, async, () -> {
            spec.beginControlFlow("try ($T connection = dataSource.getConnection())", Connection.class);
            spec.beginControlFlow(
                    "try ($T statement = connection.prepareStatement($S))", PreparedStatement.class, query);

            // if it doesn't have a By section, we add all the requested columns
            var parameterName = info.parameterName(0);
            int variableIndex = 0;
            for (ColumnInfo column : columns) {
                var columnName = column.name();
                var columnType = column.typeName();

                var input = "%s.%s()".formatted(parameterName, columnName);
                if (TypeUtils.needsTypeCodec(columnType)) {
                    input = CodeBlock.of("this.__$L.encode($L)", columnName, input)
                            .toString();
                }
                // jdbc index starts at 1
                spec.addStatement(jdbcSetFor(columnType, "statement.%s($L, $L)"), ++variableIndex, input);
            }

            content.run();

            spec.endControlFlow();
            spec.nextControlFlow("catch ($T exception)", SQLException.class);
            spec.addStatement("throw new $T($S, exception)", CompletionException.class, "Unexpected error occurred");
            spec.endControlFlow();
        });
        typeSpec.addMethod(spec.build());
    }

    private void addBySectionData(
            MethodSpec.Builder spec, boolean async, String query, QueryInfo info, Runnable content) {
        wrapInCompletableFuture(spec, async, () -> {
            spec.beginControlFlow("try ($T connection = dataSource.getConnection())", Connection.class);
            spec.beginControlFlow(
                    "try ($T statement = connection.prepareStatement($S))", PreparedStatement.class, query);

            // if it has a By section, everything is handled through the parameters
            int variableIndex = 0;
            for (VariableByFactor variable : info.byVariables()) {
                var columnName = variable.name();
                var columnType =
                        Objects.requireNonNull(info.columnFor(columnName)).typeName();

                for (@NonNull CharSequence parameterName : variable.keyword().parameterNames()) {
                    var input = parameterName;
                    if (TypeUtils.needsTypeCodec(columnType)) {
                        input = CodeBlock.of("this.__$L.encode($L)", columnName, input)
                                .toString();
                    }
                    // jdbc index starts at 1
                    spec.addStatement(jdbcSetFor(columnType, "statement.%s($L, $L)"), ++variableIndex, input);
                }
            }

            content.run();

            spec.endControlFlow();
            spec.nextControlFlow("catch ($T exception)", SQLException.class);
            spec.addStatement("throw new $T($S, exception)", CompletionException.class, "Unexpected error occurred");
            spec.endControlFlow();
        });
        typeSpec.addMethod(spec.build());
    }

    private String createSetFor(QueryInfo info) {
        return createParametersForColumns(info.entityInfo().notKeyColumns(), null, ',');
    }

    private String createWhereForKeys(QueryInfo info) {
        return createParametersForColumns(info.entityInfo().keyColumns(), () -> AndFactor.INSTANCE, ' ');
    }

    private String createWhereForAll(QueryInfo info) {
        return createParametersForFactors(info.bySectionFactors(), ' ');
    }

    private String createParametersForColumns(
            List<ColumnInfo> columns, Supplier<Factor> factorSeparator, char separator) {
        var factors = new ArrayList<Factor>();
        for (ColumnInfo column : columns) {
            if (!factors.isEmpty() && factorSeparator != null) {
                factors.add(factorSeparator.get());
            }
            factors.add(new VariableByFactor(column.name()));
        }
        return createParametersForFactors(factors, separator);
    }

    private String createParametersForFactors(List<Factor> factors, char separator) {
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

            builder.append(variable.name());
            if (keyword instanceof EqualsKeyword) {
                builder.append("=?");
            } else if (keyword instanceof LessThanKeyword) {
                builder.append("<?");
            } else {
                throw new InvalidRepositoryException("Unsupported keyword %s", keyword);
            }
        }
        return builder.toString();
    }
}
