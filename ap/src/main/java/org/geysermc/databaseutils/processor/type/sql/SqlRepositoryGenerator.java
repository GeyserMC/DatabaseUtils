/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.type.sql;

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
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import org.geysermc.databaseutils.DatabaseCategory;
import org.geysermc.databaseutils.processor.info.ColumnInfo;
import org.geysermc.databaseutils.processor.query.QueryContext;
import org.geysermc.databaseutils.processor.query.section.by.keyword.EqualsKeyword;
import org.geysermc.databaseutils.processor.query.section.by.keyword.LessThanKeyword;
import org.geysermc.databaseutils.processor.query.section.by.keyword.NotNullKeyword;
import org.geysermc.databaseutils.processor.query.section.by.keyword.NullKeyword;
import org.geysermc.databaseutils.processor.query.section.factor.AndFactor;
import org.geysermc.databaseutils.processor.query.section.factor.Factor;
import org.geysermc.databaseutils.processor.query.section.factor.OrFactor;
import org.geysermc.databaseutils.processor.query.section.factor.VariableByFactor;
import org.geysermc.databaseutils.processor.query.section.projection.ProjectionKeyword;
import org.geysermc.databaseutils.processor.query.section.projection.keyword.AvgProjectionKeyword;
import org.geysermc.databaseutils.processor.query.section.projection.keyword.FirstProjectionKeyword;
import org.geysermc.databaseutils.processor.query.section.projection.keyword.SkipProjectionKeyword;
import org.geysermc.databaseutils.processor.query.section.projection.keyword.TopProjectionKeyword;
import org.geysermc.databaseutils.processor.type.RepositoryGenerator;
import org.geysermc.databaseutils.processor.type.sql.QueryBuilder.QueryBuilderColumn;
import org.geysermc.databaseutils.processor.util.InvalidRepositoryException;
import org.geysermc.databaseutils.processor.util.TypeUtils;
import org.geysermc.databaseutils.sql.SqlDialect;

public final class SqlRepositoryGenerator extends RepositoryGenerator {
    private static final int BATCH_SIZE = 500;

    public SqlRepositoryGenerator() {
        super(DatabaseCategory.SQL);
    }

    @Override
    protected void onConstructorBuilder(MethodSpec.Builder builder) {
        typeSpec.addField(HikariDataSource.class, "dataSource", Modifier.PRIVATE, Modifier.FINAL);
        builder.addStatement("this.dataSource = database.dataSource()");

        typeSpec.addField(SqlDialect.class, "dialect", Modifier.PRIVATE, Modifier.FINAL);
        builder.addStatement("this.dialect = database.dialect()");
    }

    @Override
    public void addFind(QueryContext context, MethodSpec.Builder spec) {
        var builder = new QueryBuilder(context)
                .add("select %s", this::createProjectionFor)
                .addRaw("from %s", context.tableName());
        if (context.hasBySection()) {
            builder.add("where %s", this::createWhereForFactors);
        } else if (context.hasParameters()) {
            builder.add("where %s", this::createWhereForKeys);
        }
        executeAndReturn(spec, context, builder);
    }

    @Override
    public void addExists(QueryContext context, MethodSpec.Builder spec) {
        var builder = new QueryBuilder(context).addRaw("select 1 from %s", context.tableName());
        if (context.hasBySection()) {
            builder.add("where %s", this::createWhereForFactors);
        } else if (context.hasParameters()) {
            builder.add("where %s", this::createWhereForKeys);
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
        } else if (context.hasParameters()) {
            builder.add("where %s", this::createWhereForKeys);
        }

        if (context.hasProjection()) {
            var limit = context.projection().limit();
            if (limit != -1) {
                builder.addRaw("limit " + limit);
            }
        }

        boolean notSelfToSelf =
                !context.parametersInfo().isSelf() && context.returnInfo().isAnySelf();
        if (!notSelfToSelf) {
            addUpdateQueryData(spec, context, builder);
            return;
        }

        spec.addStatement("String __sql");
        spec.beginControlFlow(
                "if (this.dialect == $T.POSTGRESQL || this.dialect == $T.SQLITE || this.dialect == $T.MARIADB)",
                SqlDialect.class,
                SqlDialect.class,
                SqlDialect.class);
        spec.addStatement("__sql = $S", builder.copy().addEndRaw("returning *"));
        spec.nextControlFlow("else if (this.dialect == $T.ORACLE_DATABASE)", SqlDialect.class);
        // todo this needs prepareStatement(__sql, new String[] {columnNames})
        // var oracleDbSqlBuilder = builder.copy()
        //         .addEndRaw(
        //                 "returning %s into %s",
        //                 mapAndJoin(context.entityInfo().columns(), ColumnInfo::name),
        //                 join(repeat("?", context.columns().size())));
        // spec.addStatement("__sql = $S", oracleDbSqlBuilder);
        spec.addStatement("throw new $T($S)", IllegalStateException.class, "This behaviour is not yet implemented!");
        spec.nextControlFlow("else if (this.dialect == $T.SQL_SERVER)", SqlDialect.class);
        spec.addStatement("__sql = $S", builder.copy().addRawBefore("where", "output deleted.*"));
        spec.nextControlFlow(
                "else if (this.dialect == $T.H2 || this.dialect == $T.MYSQL)", SqlDialect.class, SqlDialect.class);
        // todo implement this using two separate queries with a transaction
        spec.addStatement("throw new $T($S)", IllegalStateException.class, "This behaviour is not yet implemented!");
        spec.nextControlFlow("else");
        spec.addStatement("throw new $T($S + dialect)", IllegalStateException.class, "Unexpected dialect ");
        spec.endControlFlow();

        executeAndReturn(spec, context, builder.dialectDepending(true));
    }

    private void executeAndReturn(MethodSpec.Builder spec, QueryContext context, QueryBuilder builder) {
        addExecuteQueryData(spec, context, builder, () -> {
            if (context.returnInfo().isCollection()) {
                spec.addStatement(
                        "$T __responses = new $T<>()",
                        context.returnType(),
                        context.typeUtils().collectionImplementationFor(context.returnType()));
                spec.beginControlFlow("while (__result.next())");
            } else {
                spec.beginControlFlow("if (!__result.next())");
                spec.addStatement("return null");
                spec.endControlFlow();
            }

            if (context.hasProjectionColumnName()) {
                var column = context.projectionColumnInfo();

                var block = CodeBlock.builder();

                if (context.returnInfo().isCollection()) {
                    block.add("__responses.add(");
                } else {
                    block.add("return ");
                }

                var getFormat = jdbcGetFor(column.typeName(), "__result.%s");
                if (TypeUtils.needsTypeCodec(column.typeName())) {
                    getFormat = CodeBlock.of("this.__$L.decode($L)", column.name(), getFormat)
                            .toString();
                }

                block.add("%s".formatted(getFormat), column.name());

                if (context.returnInfo().isCollection()) {
                    block.add(")");
                    spec.addStatement(block.build());
                    spec.endControlFlow();
                    spec.addStatement("return __responses");
                } else {
                    spec.addStatement(block.build());
                }
                return;
            }

            var arguments = new ArrayList<String>();
            for (ColumnInfo column : context.columns()) {
                var getFormat = jdbcGetFor(column.typeName(), "__result.%s");
                if (TypeUtils.needsTypeCodec(column.typeName())) {
                    getFormat = CodeBlock.of("this.__$L.decode($L)", column.name(), getFormat)
                            .toString();
                }

                spec.addStatement("$T _$L = %s".formatted(getFormat), column.asType(), column.name(), column.name());
                arguments.add("_" + column.name());
            }

            if (context.returnInfo().isCollection()) {
                spec.addStatement(
                        "__responses.add(new $T($L))",
                        ClassName.get(context.entityType()),
                        String.join(", ", arguments));
                spec.endControlFlow();
                spec.addStatement("return __responses");
            } else {
                spec.addStatement(
                        "return new $T($L)", ClassName.get(context.entityType()), String.join(", ", arguments));
            }
        });
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
                if (context.typeUtils().isType(Integer.class, context.returnType())) {
                    spec.addStatement("return __statement.executeUpdate()");
                    return;
                } else if (context.typeUtils().isType(Boolean.class, context.returnType())) {
                    spec.addStatement("return __statement.executeUpdate() > 0");
                    return;
                } else {
                    spec.addStatement("__statement.executeUpdate()");
                }
            }

            if (context.typeUtils().isType(Void.class, context.returnType())) {
                spec.addStatement("return $L", context.returnInfo().async() ? "null" : "");
            } else if (context.returnInfo().isSelf()) {
                // todo support also creating an entity type from the given parameters
                if (context.parametersInfo().isSelf()) {
                    spec.addStatement("return $L", context.parametersInfo().firstName());
                }
                // the else has to be handled in the action. e.g.: TestEntity deleteByAAndB(int, String)
            } else if (context.typeUtils().isType(Integer.class, context.returnType())) {
                spec.addStatement("return __updateCount");
            } else if (context.typeUtils().isType(Boolean.class, context.returnType())) {
                spec.addStatement("return __updateCount > 0");
            } else {
                throw new InvalidRepositoryException(
                        "Return type can be either void, int, boolean or %s but got %s",
                        context.entityTypeName(), context.returnType());
            }
        });
    }

    private void addBySectionData(
            MethodSpec.Builder spec, QueryContext context, QueryBuilder builder, Runnable execute) {
        wrapInCompletableFuture(spec, context.returnInfo().async(), () -> {
            spec.beginControlFlow("try ($T __connection = this.dataSource.getConnection())", Connection.class);

            if (context.parametersInfo().isSelfCollection()) {
                spec.addStatement("__connection.setAutoCommit(false)");
            }

            spec.beginControlFlow(
                    "try ($T __statement = __connection.prepareStatement($L))",
                    PreparedStatement.class,
                    builder.dialectDepending() ? "__sql" : '"' + builder.query() + '"');

            CharSequence parameterName = "";
            if (context.hasParameters()) {
                parameterName = context.parametersInfo().firstName();
            }

            boolean needsUpdatedCount = context.typeUtils().isType(Integer.class, context.returnType())
                    || context.typeUtils().isType(Boolean.class, context.returnType());

            if (context.parametersInfo().isSelfCollection()) {
                spec.addStatement("int __count = 0");
                if (needsUpdatedCount) {
                    spec.addStatement("int __updateCount = 0");
                }
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

                spec.beginControlFlow("if (++__count % $L == 0)", BATCH_SIZE);
                executeBatchAndUpdateUpdateCount(spec, needsUpdatedCount);
                spec.endControlFlow();

                spec.endControlFlow();

                executeBatchAndUpdateUpdateCount(spec, needsUpdatedCount);
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

    private void executeBatchAndUpdateUpdateCount(MethodSpec.Builder spec, boolean needsUpdatedCount) {
        if (!needsUpdatedCount) {
            spec.addStatement("__statement.executeBatch()");
            return;
        }

        // todo this can also be used to check which items were and weren't inserted etc.
        spec.addStatement("int[] __affected = __statement.executeBatch()");
        spec.beginControlFlow("for (int __updated : __affected)");

        spec.beginControlFlow("if (__updated > 0)");
        spec.addStatement("__updateCount += __updated");
        spec.endControlFlow();

        spec.endControlFlow();
    }

    private String createSetFor(QueryContext context, QueryBuilder builder) {
        if (!context.parametersInfo().remaining().isEmpty()) {
            List<Factor> columns = context.parametersInfo().remaining().stream()
                    .map(item -> new VariableByFactor(item.name(), item.name()))
                    .collect(Collectors.toUnmodifiableList());
            return createParametersForFactors(columns, ',', builder, true);
        }
        var parameterName = context.parametersInfo().isSelfCollection()
                ? "__element"
                : context.parametersInfo().firstName();
        return createParametersForFactors(
                context.entityInfo().notKeyColumnsAsFactors(null, parameterName), ',', builder, false);
    }

    private String createWhereForKeys(QueryContext context, QueryBuilder builder) {
        var parameterName = context.parametersInfo().isSelfCollection()
                ? "__element"
                : context.parametersInfo().firstName();
        return createParametersForFactors(
                context.entityInfo().keyColumnsAsFactors(AndFactor.INSTANCE, parameterName), ' ', builder, false);
    }

    private String createWhereForFactors(QueryContext context, QueryBuilder builder) {
        return createParametersForFactors(context.bySectionFactors(), ' ', builder, true);
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
            } else if (keyword instanceof NullKeyword) {
                builder.append(" is null");
            } else if (keyword instanceof NotNullKeyword) {
                builder.append(" is not null");
            } else if (keyword instanceof LessThanKeyword) {
                builder.append("<?");
            } else {
                throw new InvalidRepositoryException("Unsupported keyword %s", keyword);
            }

            queryBuilder.addColumn(variable, parameter);
        }
        return builder.toString();
    }

    private String createProjectionFor(QueryContext context, QueryBuilder builder) {
        var section = context.result().projection();
        if (section == null) {
            return "*";
        }
        var distinct = section.distinct();
        var columnName = section.columnName();

        var result = columnName != null ? columnName : "*";
        if (distinct) {
            result = "distinct " + result;
        }

        for (ProjectionKeyword projection : section.nonSpecialProjectionKeywords()) {
            if (projection instanceof AvgProjectionKeyword) {
                if (!context.typeUtils().isWholeNumberType(context.returnType())) {
                    result = "avg(%s)".formatted(result);
                }
                continue;
            }
            if (projection instanceof TopProjectionKeyword keyword) {
                builder.addEndRaw("limit " + keyword.limit());
                continue;
            }
            if (projection instanceof FirstProjectionKeyword) {
                builder.addEndRaw("limit 1");
                continue;
            }
            if (projection instanceof SkipProjectionKeyword keyword) {
                builder.addEndRaw("offset " + keyword.offset());
                continue;
            }
            throw new InvalidRepositoryException("Unsupported projection %s", projection.name());
        }

        return result;
    }
}
