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
import java.util.function.Function;
import java.util.function.Supplier;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import org.geysermc.databaseutils.processor.info.ColumnInfo;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.query.QueryInfo;
import org.geysermc.databaseutils.processor.query.section.QuerySection;
import org.geysermc.databaseutils.processor.query.section.VariableSection;
import org.geysermc.databaseutils.processor.query.section.selector.AndSelector;
import org.geysermc.databaseutils.processor.query.section.selector.OrSelector;
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
    public void addFindBy(QueryInfo info, MethodSpec.Builder spec, boolean async) {
        var query = "select * from %s where %s".formatted(info.tableName(), createWhereFor(info));
        addActionedQueryData(spec, async, query, info.variableNames(), info.parameterNames(), info::columnFor, () -> {
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
    public void addExistsBy(QueryInfo info, MethodSpec.Builder spec, boolean async) {
        var query = "select 1 from %s where %s".formatted(info.tableName(), createWhereFor(info));
        addActionedQueryData(
                spec,
                async,
                query,
                info.variableNames(),
                info.parameterNames(),
                info::columnFor,
                () -> spec.addStatement("return result.next()"));
    }

    @Override
    public void addDeleteBy(QueryInfo info, MethodSpec.Builder spec, boolean async) {
        var query = "delete from %s where %s".formatted(info.tableName(), createWhereFor(info));
        addActionedUpdateData(
                spec,
                info.entityType(),
                Void.class.getCanonicalName(),
                async,
                query,
                info.variableNames(),
                info.parameterNames(),
                null,
                info::columnFor);
    }

    @Override
    public void addInsert(
            EntityInfo info,
            TypeElement returnType,
            VariableElement parameter,
            MethodSpec.Builder spec,
            boolean async) {
        var columnNames =
                String.join(",", info.columns().stream().map(ColumnInfo::name).toList());
        var columnParameters = String.join(",", repeat("?", info.columns().size()));
        var query = "insert into %s (%s) values (%s)".formatted(info.name(), columnNames, columnParameters);
        addSimple(info, returnType, parameter, query, info.columns(), spec, async);
    }

    @Override
    public void addUpdate(
            EntityInfo info,
            TypeElement returnType,
            VariableElement parameter,
            MethodSpec.Builder spec,
            boolean async) {
        var query = "update %s set %s where %s".formatted(info.name(), createSetFor(info), createWhereFor(info));
        var variables = new ArrayList<>(info.notKeyColumns());
        variables.addAll(info.keyColumns());
        addSimple(info, returnType, parameter, query, variables, spec, async);
    }

    @Override
    public void addDelete(
            EntityInfo info,
            TypeElement returnType,
            VariableElement parameter,
            MethodSpec.Builder spec,
            boolean async) {
        var query = "delete from %s where %s".formatted(info.name(), createWhereFor(info));
        addSimple(info, returnType, parameter, query, info.keyColumns(), spec, async);
    }

    private void addSimple(
            EntityInfo info,
            TypeElement returnType,
            VariableElement parameter,
            String query,
            List<ColumnInfo> variables,
            MethodSpec.Builder spec,
            boolean async) {
        var variableNames =
                variables.stream().map(column -> column.name().toString()).toList();
        var variableFormat = parameter.getSimpleName() + ".%s()";
        addActionedUpdateData(
                spec,
                info.className(),
                returnType.getQualifiedName(),
                async,
                query,
                variableNames,
                List.of(parameter.getSimpleName()),
                variableFormat,
                info::columnFor);
    }

    private void addActionedQueryData(
            MethodSpec.Builder spec,
            boolean async,
            String query,
            List<? extends CharSequence> variableNames,
            List<? extends CharSequence> parameterNames,
            Function<CharSequence, ColumnInfo> columnFor,
            Runnable content) {
        addActionedData(spec, async, query, variableNames, parameterNames, null, columnFor, () -> {
            spec.beginControlFlow("try ($T result = statement.executeQuery())", ResultSet.class);
            content.run();
            spec.endControlFlow();
        });
    }

    private void addActionedUpdateData(
            MethodSpec.Builder spec,
            CharSequence entityType,
            CharSequence returnType,
            boolean async,
            String query,
            List<? extends CharSequence> variableNames,
            List<? extends CharSequence> parameterNames,
            String variableFormat,
            Function<CharSequence, ColumnInfo> columnFor) {
        addActionedData(spec, async, query, variableNames, parameterNames, variableFormat, columnFor, () -> {
            spec.addStatement("statement.executeUpdate()");
            if (TypeUtils.isTypeOf(Void.class, returnType)) {
                spec.addStatement("return null");
            } else if (TypeUtils.isTypeOf(entityType, returnType)) {
                // todo support also creating an entity type from the given parameters
                spec.addStatement("return $L", parameterNames.get(0));
            } else {
                throw new InvalidRepositoryException(
                        "Return type can be either void or %s but got %s", entityType, returnType);
            }
        });
    }

    private void addActionedData(
            MethodSpec.Builder spec,
            boolean async,
            String query,
            List<? extends CharSequence> variableNames,
            List<? extends CharSequence> parameterNames,
            String variableFormat,
            Function<CharSequence, ColumnInfo> columnFor,
            Runnable content) {
        wrapInCompletableFuture(spec, async, () -> {
            spec.beginControlFlow("try ($T connection = dataSource.getConnection())", Connection.class);
            spec.beginControlFlow(
                    "try ($T statement = connection.prepareStatement($S))", PreparedStatement.class, query);

            for (int i = 0; i < variableNames.size(); i++) {
                var name = variableNames.get(i);
                var columnType = columnFor.apply(name).typeName();
                var input = variableFormat != null ? variableFormat.formatted(name) : parameterNames.get(i);

                if (TypeUtils.needsTypeCodec(columnType)) {
                    input = CodeBlock.of("this.__$L.encode($L)", name, input).toString();
                }

                spec.addStatement(jdbcSetFor(columnType, "statement.%s($L, $L)"), i + 1, input);
            }

            content.run();

            spec.endControlFlow();
            spec.nextControlFlow("catch ($T exception)", SQLException.class);
            spec.addStatement("throw new $T($S, exception)", CompletionException.class, "Unexpected error occurred");
            spec.endControlFlow();
        });
        typeSpec.addMethod(spec.build());
    }

    private String createSetFor(EntityInfo info) {
        return createParametersForColumns(info.notKeyColumns(), null, ',');
    }

    private String createWhereFor(EntityInfo info) {
        return createParametersForColumns(info.keyColumns(), () -> AndSelector.INSTANCE, ' ');
    }

    private String createWhereFor(QueryInfo info) {
        return createParametersForSections(info.sections(), ' ');
    }

    private String createParametersForColumns(
            List<ColumnInfo> columns, Supplier<QuerySection> sectionSeparator, char separator) {
        var sections = new ArrayList<QuerySection>();
        for (ColumnInfo column : columns) {
            if (!sections.isEmpty() && sectionSeparator != null) {
                sections.add(sectionSeparator.get());
            }
            sections.add(new VariableSection(column.name()));
        }
        return createParametersForSections(sections, separator);
    }

    private String createParametersForSections(List<QuerySection> sections, char separator) {
        var builder = new StringBuilder();
        for (QuerySection section : sections) {
            if (!builder.isEmpty()) {
                builder.append(separator);
            }

            if (section instanceof VariableSection variable) {
                builder.append(variable.name()).append("=?");
            } else if (section instanceof AndSelector) {
                builder.append("and");
            } else if (section instanceof OrSelector) {
                builder.append("or");
            } else {
                throw new InvalidRepositoryException(
                        "Unknown action type %s", section.getClass().getCanonicalName());
            }
        }
        return builder.toString();
    }
}
