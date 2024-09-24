/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.type.sql;

import static org.geysermc.databaseutils.processor.type.sql.JdbcTypeMappingRegistry.jdbcGetFor;
import static org.geysermc.databaseutils.processor.type.sql.JdbcTypeMappingRegistry.jdbcReadFor;
import static org.geysermc.databaseutils.processor.type.sql.JdbcTypeMappingRegistry.jdbcSetFor;
import static org.geysermc.databaseutils.processor.util.CollectionUtils.mapAndJoin;
import static org.geysermc.databaseutils.processor.util.StringUtils.repeat;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import oracle.jdbc.OracleTypes;
import org.geysermc.databaseutils.DatabaseCategory;
import org.geysermc.databaseutils.processor.info.ColumnInfo;
import org.geysermc.databaseutils.processor.info.EntityInfo;
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
import org.geysermc.databaseutils.processor.type.sql.repository.DialectClassManager;
import org.geysermc.databaseutils.processor.type.sql.repository.DialectMethod;
import org.geysermc.databaseutils.processor.type.sql.repository.DialectMethod.Identifier;
import org.geysermc.databaseutils.processor.util.InvalidRepositoryException;
import org.geysermc.databaseutils.processor.util.TypeUtils;
import org.geysermc.databaseutils.sql.FlexibleSqlInput;
import org.geysermc.databaseutils.sql.SqlDialect;

public final class SqlRepositoryGenerator extends RepositoryGenerator {
    private static final int BATCH_SIZE = 500;
    private DialectClassManager dialectManager;

    public SqlRepositoryGenerator() {
        super(DatabaseCategory.SQL);
    }

    @Override
    public void init(TypeElement superType, EntityInfo entityInfo) {
        super.init(superType, entityInfo);
        dialectManager = new DialectClassManager(typeSpec, className());
    }

    @Override
    protected void onConstructorBuilder(MethodSpec.Builder builder) {
        typeSpec.addField(HikariDataSource.class, "dataSource", Modifier.PRIVATE, Modifier.FINAL);
        builder.addStatement("this.dataSource = database.dataSource()");

        typeSpec.addField(SqlDialect.class, "dialect", Modifier.PRIVATE, Modifier.FINAL);
        builder.addStatement("this.dialect = database.dialect()");

        dialectManager.onConstructorBuilder(builder);
    }

    @Override
    public TypeSpec.Builder finish(Class<?> databaseClass) {
        super.finish(databaseClass);
        for (TypeSpec dialectImpl : dialectManager.finish()) {
            typeSpec.addType(dialectImpl);
        }
        return typeSpec;
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
        executeAndReturn(new DialectMethod(spec), context, builder);
    }

    @Override
    public void addExists(QueryContext context, MethodSpec.Builder spec) {
        var builder = new QueryBuilder(context).addRaw("select 1 from %s", context.tableName());
        if (context.hasBySection()) {
            builder.add("where %s", this::createWhereForFactors);
        } else if (context.hasParameters()) {
            builder.add("where %s", this::createWhereForKeys);
        }
        var method = new DialectMethod(spec);
        addExecuteQueryData(method, context, builder, () -> method.addStatement("return __result.next()"));
    }

    @Override
    public void addInsert(QueryContext context, MethodSpec.Builder spec) {
        var columnNames = String.join(
                ",", context.columns().stream().map(ColumnInfo::name).toList());
        var columnParameters = String.join(",", repeat("?", context.columns().size()));

        var builder = new QueryBuilder(context)
                .addRaw("insert into %s (%s) values (%s)", context.tableName(), columnNames, columnParameters)
                .addAll(context.columns());
        addUpdateQueryData(new DialectMethod(spec), context, builder);
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
        addUpdateQueryData(new DialectMethod(spec), context, builder);
    }

    @Override
    public void addDelete(QueryContext context, MethodSpec.Builder spec) {
        // https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/DELETE.html
        // https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/ROWNUM-Pseudocolumn.html for limit
        // https://learn.microsoft.com/en-us/sql/t-sql/statements/delete-transact-sql?view=sql-server-ver16
        // https://www.sqlite.org/lang_delete.html
        // https://h2database.com/html/commands.html#delete
        // https://dev.mysql.com/doc/refman/8.4/en/delete.html
        // https://mariadb.com/kb/en/delete/

        // returning the deleted row(s) is not supported by: h2, mysql. Resulting in delete with a find subquery

        // order by is not supported by: oracledb, mssql, h2, sqlite (not enabled in xerial)

        // limit is not supported by: sqlite (not enabled), oracledb
        // limit needs to be "top x" on mssql, needs to be "fetch next x rows" for h2

        // truncate table is supported by all dialects but sqlite
        // https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/TRUNCATE-TABLE.html
        // https://learn.microsoft.com/en-us/sql/t-sql/statements/truncate-table-transact-sql?view=sql-server-ver16
        // https://www.sqlite.org/lang_delete.html#the_truncate_optimization (basically delete from %s equals truncate)
        // https://h2database.com/html/commands.html#truncate_table
        // https://dev.mysql.com/doc/refman/8.4/en/truncate-table.html
        // https://mariadb.com/kb/en/truncate-table/
        // Only regular data tables without foreign key constraints can be truncated (source: H2, MySQL docs)
        // todo keep in mind that this will ofc. also reset the AUTO_INCREMENT counter
        // make it not support int/bool return types, because that'd require an additional select count(*)

        // todo add something that can show you which permissions you need from the db

        var query = new QueryBuilder(context).addRaw("delete from %s", context.tableName());
        if (context.hasBySection()) {
            query.add("where %s", this::createWhereForFactors);
        } else if (context.hasParameters()) {
            query.add("where %s", this::createWhereForKeys);
        }
        // todo use 'truncate table' for dialects supporting it

        if (context.hasProjection()) {
            var limit = context.projection().limit();
            if (limit != -1) {
                // todo is top for mssql
                query.addRaw("limit " + limit);
            }
        }

        boolean needsReturning =
                !context.parametersInfo().isSelf() && context.returnInfo().isAnySelf();
        if (!needsReturning) {
            addUpdateQueryData(new DialectMethod(spec), context, query);
            return;
        }

        var manager = dialectManager.create(context, spec);
        // for Postgres, SQLite and MariaDB
        manager.createDefault(builder -> {
            // todo if returning is needed and a projection column name is given, only request that specific column
            executeAndReturn(builder, context, query.copy().addEndRaw("returning *"));
        });

        manager.create(SqlDialect.SQL_SERVER, builder -> {
            // https://learn.microsoft.com/en-us/sql/t-sql/queries/output-clause-transact-sql?view=sql-server-ver16
            executeAndReturn(builder, context, query.copy().addRawBefore("where", "output deleted.*"));
        });

        manager.create(SqlDialect.ORACLE_DATABASE, builder -> {
            boolean bulk = context.returnInfo().isCollection();
            String into = bulk ? "bulk collect into" : "into";
            var oracleDbSqlBuilder = query.copy()
                    .addEndRaw(
                            "returning %s_row(%s) %s ?",
                            context.tableName(), mapAndJoin(context.entityInfo().columns(), ColumnInfo::name), into);

            addBySectionData(builder, context, oracleDbSqlBuilder, () -> {
                int set = query.columns().size() + 1;
                // has to be uppercase, in OracleDB everything is in caps by default.
                // But for the out parameter type it's not automatically converted to caps.
                if (bulk) {
                    builder.addStatement(
                            "__statement.registerOutParameter($L, $L, $S)",
                            set,
                            OracleTypes.ARRAY,
                            (context.tableName() + "_table").toUpperCase(Locale.ROOT));
                } else {
                    builder.addStatement(
                            "__statement.registerOutParameter($L, $L, $S)",
                            set,
                            OracleTypes.STRUCT,
                            (context.tableName() + "_row").toUpperCase(Locale.ROOT));
                }
                builder.addStatement("__statement.execute()");
                if (bulk) {
                    builder.addStatement(
                            "var __result = ($T[]) __statement.getArray($L).getArray()", Object.class, set);
                } else {
                    builder.addStatement("var __result = __statement.getObject($L)", set);
                }
                readStructResult(builder, context);
            });

            // need to wrap it in a BEGIN END because otherwise jdbc expects a ResultSet
            builder.replaceBeginControlFlow(
                    Identifier.PREPARE_STATEMENT,
                    "try ($T __statement = __connection.prepareCall($S))",
                    CallableStatement.class,
                    "BEGIN " + oracleDbSqlBuilder + "; END;");
        });

        manager.create(List.of(SqlDialect.H2, SqlDialect.MYSQL), builder -> {
            builder.setThrow(IllegalStateException.class, "This behaviour is not yet implemented!");
        });
    }

    private void executeAndReturn(DialectMethod spec, QueryContext context, QueryBuilder builder) {
        addExecuteQueryData(spec, context, builder, () -> {
            readResultBase(
                    spec,
                    context,
                    () -> {
                        if (context.returnInfo().isCollection()) {
                            spec.beginControlFlow("while (__result.next())");
                        } else {
                            spec.beginControlFlow("if (!__result.next())");
                            spec.addStatement("return null");
                            spec.endControlFlow();
                        }
                    },
                    (column) -> jdbcGetFor(column.typeName(), "__result.%s", column.name()));
        });
    }

    private void readStructResult(DialectMethod spec, QueryContext context) {
        readResultBase(
                spec,
                context,
                () -> {
                    if (context.returnInfo().isCollection()) {
                        spec.beginControlFlow("for (var __item : __result)");
                        spec.addStatement(
                                "var __data = new $T((($T) __item).getAttributes())",
                                FlexibleSqlInput.class,
                                Struct.class);
                    } else {
                        spec.beginControlFlow("if (__result == null)");
                        spec.addStatement("return null");
                        spec.endControlFlow();
                        spec.addStatement(
                                "var __data = new $T((($T) __result).getAttributes())",
                                FlexibleSqlInput.class,
                                Struct.class);
                    }
                },
                column -> jdbcReadFor(column.typeName(), "__data.%s"));
    }

    private void readResultBase(
            DialectMethod spec, QueryContext context, Runnable initCode, Function<ColumnInfo, String> jdbcBaseFormat) {
        if (context.returnInfo().isCollection()) {
            spec.addStatement(
                    "$T __responses = new $T<>()",
                    context.returnType(),
                    context.typeUtils().collectionImplementationFor(context.returnType()));
        }

        initCode.run();

        if (context.hasProjectionColumnName()) {
            var column = context.projectionColumnInfo();

            var block = CodeBlock.builder();

            if (context.returnInfo().isCollection()) {
                block.add("__responses.add(");
            } else {
                block.add("return ");
            }

            var format = jdbcBaseFormat.apply(column);
            if (TypeUtils.needsTypeCodec(column.typeName())) {
                format = "this.__%s.decode(%s)".formatted(column.name(), format);
            }
            block.add(format);

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
            var format = jdbcBaseFormat.apply(column);
            if (TypeUtils.needsTypeCodec(column.typeName())) {
                format = "this.__%s.decode(%s)".formatted(column.name(), format);
            }

            spec.addStatement("$T _$L = $L", column.asType(), column.name(), format);
            arguments.add("_" + column.name());
        }

        if (context.returnInfo().isCollection()) {
            spec.addStatement(
                    "__responses.add(new $T($L))", ClassName.get(context.entityType()), String.join(", ", arguments));
            spec.endControlFlow();
            spec.addStatement("return __responses");
        } else {
            spec.addStatement("return new $T($L)", ClassName.get(context.entityType()), String.join(", ", arguments));
        }
    }

    private void addExecuteQueryData(DialectMethod spec, QueryContext context, QueryBuilder builder, Runnable content) {
        addBySectionData(spec, context, builder, () -> {
            spec.beginControlFlow("try ($T __result = __statement.executeQuery())", ResultSet.class);
            content.run();
            spec.endControlFlow();
        });
    }

    private void addUpdateQueryData(DialectMethod spec, QueryContext context, QueryBuilder builder) {
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

    private void addBySectionData(DialectMethod spec, QueryContext context, QueryBuilder builder, Runnable execute) {
        wrapInCompletableFuture(spec, context.returnInfo().async(), () -> {
            spec.beginControlFlow("try ($T __connection = this.dataSource.getConnection())", Connection.class);

            if (context.parametersInfo().isSelfCollection()) {
                spec.addStatement("__connection.setAutoCommit(false)");
            }

            spec.beginControlFlow(
                    Identifier.PREPARE_STATEMENT,
                    "try ($T __statement = __connection.prepareStatement($S))",
                    PreparedStatement.class,
                    builder.query());

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
                spec.addStatement(jdbcSetFor(columnInfo.typeName(), "__statement.%s", ++variableIndex, input));
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

        // todo remove
        if (spec.shouldAdd()) {
            typeSpec.addMethod(spec.build());
        }
    }

    private void executeBatchAndUpdateUpdateCount(DialectMethod spec, boolean needsUpdatedCount) {
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

    private void wrapInCompletableFuture(DialectMethod builder, boolean async, Runnable content) {
        hasAsync |= async;

        if (async) {
            builder.beginControlFlow("return $T.supplyAsync(() ->", CompletableFuture.class);
        }
        content.run();
        if (async) {
            builder.endControlFlow(", this.database.executorService())");
        }
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
