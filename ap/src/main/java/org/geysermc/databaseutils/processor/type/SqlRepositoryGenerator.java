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

import ca.krasnay.sqlbuilder.Predicate;
import ca.krasnay.sqlbuilder.Predicates;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import javax.lang.model.element.Modifier;
import org.geysermc.databaseutils.processor.info.ColumnInfo;
import org.geysermc.databaseutils.processor.query.QueryInfo;
import org.geysermc.databaseutils.processor.query.section.QuerySection;
import org.geysermc.databaseutils.processor.query.section.VariableSection;
import org.geysermc.databaseutils.processor.query.section.selector.AndSelector;
import org.geysermc.databaseutils.processor.query.section.selector.OrSelector;
import org.geysermc.databaseutils.processor.type.sql.CustomSelectCreator;
import org.geysermc.databaseutils.processor.util.InvalidRepositoryException;

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
    public void addFindBy(QueryInfo queryInfo, MethodSpec.Builder spec, boolean async) {
        var queryCreator = new CustomSelectCreator().where(createPredicateFor(queryInfo));
        addActionedData(queryInfo, spec, async, queryCreator, () -> {
            spec.beginControlFlow("if (!result.next())");
            spec.addStatement("return null");
            spec.endControlFlow();

            var arguments = new ArrayList<String>();
            for (ColumnInfo column : queryInfo.columns()) {
                var columnType = ClassName.bestGuess(column.typeName().toString());
                spec.addStatement(
                        jdbcGetFor(column.typeName(), "$T _$L = result.%s(%s)"),
                        columnType,
                        column.name(),
                        column.name());
                arguments.add("_" + column.name());
            }
            spec.addStatement(
                    "return new $T($L)",
                    ClassName.bestGuess(queryInfo.entityType().toString()),
                    String.join(", ", arguments));
        });
    }

    @Override
    public void addExistsBy(QueryInfo queryInfo, MethodSpec.Builder spec, boolean async) {
        var queryCreator = new CustomSelectCreator().column("1").where(createPredicateFor(queryInfo));
        addActionedData(queryInfo, spec, async, queryCreator, () -> spec.addStatement("return result.next()"));
    }

    @Override
    public void addSimple(String actionType, QueryInfo queryInfo, MethodSpec.Builder spec, boolean async) {

    }

    private void addActionedData(
            QueryInfo queryInfo,
            MethodSpec.Builder spec,
            boolean async,
            CustomSelectCreator queryCreator,
            Runnable content) {
        wrapInCompletableFuture(spec, async, () -> {
            var specResult = queryCreator.from(queryInfo.tableName()).toSpecResult();
            var query = specResult.query();

            spec.beginControlFlow("try ($T connection = dataSource.getConnection())", Connection.class);
            spec.beginControlFlow(
                    "try ($T statement = connection.prepareStatement($L))", PreparedStatement.class, query);

            var parameterNames = specResult.parameterNames();
            for (int i = 0; i < parameterNames.size(); i++) {
                var name = parameterNames.get(i);
                var columnType = queryInfo.columnFor(name).typeName();
                spec.addStatement(jdbcSetFor(columnType, "statement.%s($L, $L)"), i + 1, parameterNames.get(i));
            }

            spec.beginControlFlow("try ($T result = statement.executeQuery())", ResultSet.class);
            content.run();
            spec.endControlFlow();

            spec.endControlFlow();
            spec.nextControlFlow("catch ($T exception)", SQLException.class);
            spec.addStatement("throw new $T($S, exception)", CompletionException.class, "Unexpected error occurred");
            spec.endControlFlow();
        });
        typeSpec.addMethod(spec.build());
    }

    private Predicate createPredicateFor(QueryInfo info) {
        var sections = new ArrayList<>(info.sections());

        // infix to prefix
        // AOrBAndC -> OrAAndBC
        for (int i = 0; i < sections.size(); i++) {
            var section = sections.get(i);
            if (!(section instanceof VariableSection)) {
                // this is fine because the index doesn't shift
                //noinspection SuspiciousListRemoveInLoop
                sections.remove(i);
                sections.add(i - 1, section);
            }
        }

        return createPredicateFor(info, sections, 0, 0).predicate;
    }

    private PredicateStep createPredicateFor(QueryInfo info, List<QuerySection> sections, int index, int varIndex) {
        var section = sections.get(index);
        if (section instanceof VariableSection variable) {
            return new PredicateStep(
                    Predicates.eq(
                            variable.name(), info.parameters().get(varIndex).getSimpleName()),
                    index,
                    varIndex + 1);
        } else if (section instanceof AndSelector) {
            var left = createPredicateFor(info, sections, index + 1, varIndex);
            var right = createPredicateFor(info, sections, left.index + 1, left.varIndex);
            return new PredicateStep(Predicates.and(left.predicate, right.predicate), right.index, right.varIndex);
        } else if (section instanceof OrSelector) {
            var left = createPredicateFor(info, sections, index + 1, varIndex);
            var right = createPredicateFor(info, sections, left.index + 1, left.varIndex);
            return new PredicateStep(Predicates.or(left.predicate, right.predicate), right.index, right.varIndex);
        } else {
            throw new InvalidRepositoryException(
                    "Unknown action type %s", section.getClass().getCanonicalName());
        }
    }

    private record PredicateStep(Predicate predicate, int index, int varIndex) {}
}
