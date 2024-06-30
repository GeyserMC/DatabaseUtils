/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.type.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.DeleteOneModel;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.WriteModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.Modifier;
import org.bson.Document;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.databaseutils.DatabaseType;
import org.geysermc.databaseutils.processor.query.QueryContext;
import org.geysermc.databaseutils.processor.query.section.by.keyword.EqualsKeyword;
import org.geysermc.databaseutils.processor.query.section.by.keyword.LessThanKeyword;
import org.geysermc.databaseutils.processor.query.section.factor.AndFactor;
import org.geysermc.databaseutils.processor.query.section.factor.Factor;
import org.geysermc.databaseutils.processor.query.section.factor.OrFactor;
import org.geysermc.databaseutils.processor.query.section.factor.VariableByFactor;
import org.geysermc.databaseutils.processor.query.section.factor.VariableOrderByFactor;
import org.geysermc.databaseutils.processor.query.section.projection.keyword.SkipProjectionKeyword;
import org.geysermc.databaseutils.processor.query.section.projection.keyword.TopProjectionKeyword;
import org.geysermc.databaseutils.processor.type.RepositoryGenerator;
import org.geysermc.databaseutils.processor.util.InvalidRepositoryException;

public class MongoRepositoryGenerator extends RepositoryGenerator {
    public MongoRepositoryGenerator() {
        super(DatabaseType.MONGODB);
    }

    @Override
    protected void onConstructorBuilder(MethodSpec.Builder builder) {
        typeSpec.addField(
                ParameterizedTypeName.get(ClassName.get(MongoCollection.class), ClassName.get(entityInfo.type())),
                "collection",
                Modifier.PRIVATE,
                Modifier.FINAL);

        builder.addStatement(
                "this.collection = database.mongoDatabase().getCollection($S, $T.class)",
                entityInfo.name(),
                entityInfo.type());
    }

    @Override
    public void addFind(QueryContext context, MethodSpec.Builder spec) {
        // todo wrap in exception handlers, for all methods
        wrapInCompletableFuture(spec, context.returnInfo().async(), () -> {
            spec.addStatement(
                    "return this.collection.find($L)$L$L",
                    createFilter(context.bySectionFactors()),
                    createSort(context),
                    createProjection(context));
        });
        typeSpec.addMethod(spec.build());
    }

    @Override
    public void addExists(QueryContext context, MethodSpec.Builder spec) {
        wrapInCompletableFuture(spec, context.returnInfo().async(), () -> {
            spec.addStatement(
                    "return this.collection.find($L)$L.limit(1)$L != null",
                    createFilter(context.bySectionFactors()),
                    createSort(context),
                    createProjection(context));
        });
        typeSpec.addMethod(spec.build());
    }

    @Override
    public void addInsert(QueryContext context, MethodSpec.Builder spec) {
        wrapInCompletableFuture(spec, context.returnInfo().async(), () -> {
            if (context.parametersInfo().isSelfCollection()) {
                spec.addStatement(
                        "this.collection.insertMany($L)",
                        context.parametersInfo().firstName());
            } else if (context.parametersInfo().isSelf()) {
                spec.addStatement(
                        "this.collection.insertOne($L)",
                        context.parametersInfo().firstName());
            } else {
                throw new InvalidRepositoryException("Expected insert to be either self or a collection of self");
            }

            // todo support getInsertedIds()
            if (context.returnInfo().async()) {
                spec.addStatement("return null");
            }
        });
        typeSpec.addMethod(spec.build());
    }

    @Override
    public void addUpdate(QueryContext context, MethodSpec.Builder spec) {
        // todo support projection having multiple columns
        wrapInCompletableFuture(spec, context.returnInfo().async(), () -> {
            // for now, it's only either: update a (list of) entities, or updateAByBAndC
            // todo keep track of which fields are changed to make sure we only update the fields who have been changed
            // instead of replacing the documents

            if (context.parametersInfo().isSelf()) {
                var name = context.parametersInfo().firstName();
                spec.addStatement(
                        "this.collection.replaceOne($L, $L)",
                        createFilter(context.entityInfo().keyColumnsAsFactors(AndFactor.INSTANCE, name)),
                        name);
            } else if (context.parametersInfo().isSelfCollection()) {
                spec.addStatement(
                        "var __bulkOperations = new $T<$T<$T>>()",
                        ArrayList.class,
                        WriteModel.class,
                        context.entityType());

                spec.beginControlFlow(
                        "for (var __entry : $L)", context.parametersInfo().firstName());
                spec.addStatement(
                        "__bulkOperations.add(new $T<>($L, $L))",
                        ReplaceOneModel.class,
                        createFilter(context.entityInfo().keyColumnsAsFactors(AndFactor.INSTANCE, "__entry")),
                        "__entry");
                spec.endControlFlow();

                spec.addStatement("this.collection.bulkWrite(__bulkOperations)");
            } else if (context.projection() != null && context.projection().columnName() != null) {
                spec.addStatement(
                        "this.collection.updateMany($L, new $T($S, $L))",
                        createFilter(context.bySectionFactors()),
                        Document.class,
                        context.projection().columnName(),
                        context.parametersInfo().columnParameter());
            } else {
                throw new InvalidRepositoryException(
                        "Expected either a list of entities to update, an entity or a field to update for %s",
                        context.parametersInfo().element());
            }

            if (context.returnInfo().async()) {
                spec.addStatement("return null");
            }
        });
        typeSpec.addMethod(spec.build());
    }

    @Override
    public void addDelete(QueryContext context, MethodSpec.Builder spec) {
        wrapInCompletableFuture(spec, context.returnInfo().async(), () -> {
            // for now, it's only either: delete a (list of) entities, or deleteByAAndB
            if (context.parametersInfo().isSelf()) {
                var filter = createFilter(context.entityInfo()
                        .keyColumnsAsFactors(
                                AndFactor.INSTANCE, context.parametersInfo().firstName()));
                spec.addStatement("this.collection.deleteOne($L)", filter);
            } else if (context.parametersInfo().isSelfCollection()) {
                spec.addStatement(
                        "var __bulkOperations = new $T<$T<$T>>()",
                        ArrayList.class,
                        WriteModel.class,
                        context.entityType());

                spec.beginControlFlow(
                        "for (var __entry : $L)", context.parametersInfo().firstName());
                spec.addStatement(
                        "__bulkOperations.add(new $T<>($L))",
                        DeleteOneModel.class,
                        createFilter(context.entityInfo().keyColumnsAsFactors(AndFactor.INSTANCE, "__entry")));
                spec.endControlFlow();

                spec.addStatement("this.collection.bulkWrite(__bulkOperations)");
            } else {
                spec.addStatement("this.collection.deleteMany($L)", createFilter(context.bySectionFactors()));
            }

            if (context.returnInfo().async()) {
                spec.addStatement("return null");
            }
        });
        typeSpec.addMethod(spec.build());
    }

    private CodeBlock createFilter(List<Factor> factors) {
        if (factors == null || factors.isEmpty()) {
            return CodeBlock.of("$T.empty()", Filters.class);
        }

        var builder = CodeBlock.builder();
        createFilterSection(infixToPrefixFactors(factors), 0, builder);
        return builder.build();
    }

    private static ArrayList<Factor> infixToPrefixFactors(List<Factor> factors) {
        var filterOrder = new ArrayList<Factor>();

        // infix -> (reverse) postfix
        Factor lastFactor = null;
        for (int i = factors.size() - 1; i >= 0; i--) {
            var factor = factors.get(i);
            if (factor instanceof VariableByFactor) {
                filterOrder.add(factor);
                if (lastFactor != null) {
                    filterOrder.add(lastFactor);
                    lastFactor = null;
                }
                continue;
            }
            lastFactor = factor;
        }

        // (reverse) postfix -> prefix
        Collections.reverse(filterOrder);
        return filterOrder;
    }

    private int createFilterSection(List<Factor> filterOrder, int index, CodeBlock.Builder builder) {
        var factor = filterOrder.get(index);

        if (factor instanceof VariableByFactor variable) {
            var keyword = variable.keyword();

            if (keyword instanceof EqualsKeyword equals) {
                builder.add(
                        "$T.eq($S, $L)",
                        Filters.class,
                        variable.columnName(),
                        equals.parameterNames().get(0));
            } else if (keyword instanceof LessThanKeyword lessThan) {
                builder.add(
                        "$T.lt($S, $L)",
                        Filters.class,
                        variable.columnName(),
                        lessThan.parameterNames().get(0));
            } else {
                throw new InvalidRepositoryException("Unsupported keyword %s", keyword);
            }
            return index + 1;
        }

        if (factor instanceof AndFactor) {
            builder.add("$T.and(", Filters.class);
        } else if (factor instanceof OrFactor) {
            builder.add("$T.or(", Filters.class);
        } else {
            throw new InvalidRepositoryException("Unsupported factor %s", factor);
        }

        var newIndex = createFilterSection(filterOrder, index + 1, builder);
        builder.add(", ");
        newIndex = createFilterSection(filterOrder, newIndex, builder);
        builder.add(")");
        return newIndex;
    }

    private CodeBlock createProjection(QueryContext context) {
        var builder = CodeBlock.builder();

        if (context.projection() != null) {
            for (var projection : context.projection().notDistinctProjectionKeywords()) {
                if (projection instanceof TopProjectionKeyword keyword) {
                    builder.add(".limit($L)", keyword.limit());
                    continue;
                }
                if (projection instanceof SkipProjectionKeyword keyword) {
                    builder.add(".skip($L)", keyword.offset());
                    continue;
                }
                throw new InvalidRepositoryException("Unsupported projection %s", projection.name());
            }
        }

        if (context.returnInfo().isCollection()) {
            builder.add(".into(new $T<>())", context.typeUtils().collectionImplementationFor(context.returnType()));
        } else {
            builder.add(".first()");
        }
        return builder.build();
    }

    private CodeBlock createSort(QueryContext context) {
        // todo orderBy shouldn't allow or, only and
        var orderBy = context.result().orderBySection();
        if (orderBy == null) {
            return CodeBlock.of("");
        }

        var builder = CodeBlock.builder();
        builder.add(".sort($T.orderBy(", Sorts.class);

        var ascending = new StringBuilder();
        var descending = new StringBuilder();
        for (@NonNull Factor factor : orderBy.factors()) {
            if (factor instanceof VariableOrderByFactor variable) {
                switch (variable.direction()) {
                    case ASCENDING -> {
                        if (!ascending.isEmpty()) {
                            ascending.append(", ");
                        }
                        ascending.append('"').append(variable.columnName()).append('"');
                    }
                    case DESCENDING -> {
                        if (!descending.isEmpty()) {
                            descending.append(", ");
                        }
                        descending.append('"').append(variable.columnName()).append('"');
                    }
                }
            }
        }

        if (!ascending.isEmpty()) {
            builder.add("$T.ascending($L)", ascending);
        }
        if (!descending.isEmpty()) {
            if (!ascending.isEmpty()) {
                builder.add(", ", descending);
            }
            builder.add("$T.descending($L)", descending);
        }

        return builder.add("))").build();
    }
}
