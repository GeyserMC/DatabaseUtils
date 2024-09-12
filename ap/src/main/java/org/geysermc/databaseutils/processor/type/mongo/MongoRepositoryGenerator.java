/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.type.mongo;

import com.mongodb.client.MongoClient;
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
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.checkerframework.checker.nullness.qual.NonNull;
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
import org.geysermc.databaseutils.processor.query.section.factor.VariableOrderByFactor;
import org.geysermc.databaseutils.processor.query.section.projection.keyword.FirstProjectionKeyword;
import org.geysermc.databaseutils.processor.query.section.projection.keyword.SkipProjectionKeyword;
import org.geysermc.databaseutils.processor.query.section.projection.keyword.TopProjectionKeyword;
import org.geysermc.databaseutils.processor.type.RepositoryGenerator;
import org.geysermc.databaseutils.processor.util.InvalidRepositoryException;

public class MongoRepositoryGenerator extends RepositoryGenerator {
    public MongoRepositoryGenerator() {
        super(DatabaseCategory.MONGODB);
    }

    @Override
    protected void onConstructorBuilder(MethodSpec.Builder builder) {
        typeSpec.addField(MongoClient.class, "mongoClient", Modifier.PRIVATE, Modifier.FINAL);
        builder.addStatement("this.mongoClient = database.mongoClient()");

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
        // theoretically currently the getInsertedIds size should match the amount of documents sent,
        // since 'ordered' prevents it from inserting the remaining documents in case of a conflict
        wrapInCompletableFuture(spec, context.returnInfo().async(), () -> {
            if (context.parametersInfo().isSelfCollection()) {
                var firstName = context.parametersInfo().firstName();
                spec.beginControlFlow("if ($L.isEmpty())", firstName);

                if (context.typeUtils().isWholeNumberType(context.returnType())) {
                    spec.addStatement("return 0");
                    spec.endControlFlow();
                    spec.addStatement("return this.collection.insertMany($L).getInsertedIds().size()", firstName);
                    return;
                }

                spec.addStatement("return $L", context.returnInfo().async() ? "null" : "");
                spec.endControlFlow();
                spec.addStatement("this.collection.insertMany($L)", firstName);
            } else if (context.parametersInfo().isSelf()) {
                if (context.typeUtils().isWholeNumberType(context.returnType())) {
                    spec.addStatement(
                            "return this.collection.insertOne($L).getInsertedId() != null ? 1 : 0",
                            context.parametersInfo().firstName());
                    return;
                }
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
            } else {
                spec.addStatement(
                        "this.collection.updateMany($L, $L)",
                        createFilter(context.bySectionFactors()),
                        createDocument(context.parametersInfo().remaining()));
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
            boolean needsUpdatedCount = context.typeUtils().isType(Integer.class, context.returnType())
                    || context.typeUtils().isType(Boolean.class, context.returnType());
            if (needsUpdatedCount) {
                spec.addStatement("int __count");
            }

            // for now, it's only either: delete a (list of) entities, or deleteByAAndB
            if (context.parametersInfo().isSelf()) {
                var filter = createFilter(context.entityInfo()
                        .keyColumnsAsFactors(
                                AndFactor.INSTANCE, context.parametersInfo().firstName()));

                if (needsUpdatedCount) {
                    spec.addStatement("__count = this.collection.deleteOne($L).getDeletedCount()", filter);
                } else if (context.returnInfo().isSelf()) {
                    spec.addStatement("return this.collection.findOneAndDelete($L)", filter);
                } else {
                    spec.addStatement("this.collection.deleteOne($L)", filter);
                }
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

                if (needsUpdatedCount) {
                    spec.addStatement("__count = this.collection.bulkWrite(__bulkOperations).getDeletedCount()");
                } else {
                    spec.addStatement("this.collection.bulkWrite(__bulkOperations)");
                }
            } else {
                if (!context.hasProjection() && needsUpdatedCount) {
                    spec.addStatement(
                            "__count = (int) this.collection.deleteMany($L).getDeletedCount()",
                            createFilter(context.bySectionFactors()));
                } else {
                    var filter = createFilter(context.bySectionFactors());
                    if (context.returnInfo().isSelf()) {
                        spec.addStatement("return this.collection.findOneAndDelete($L)", filter);
                        return;
                    } else if (context.returnInfo().isSelfCollection() || needsUpdatedCount) {
                        spec.addStatement("var __session = this.mongoClient.startSession()");
                        spec.beginControlFlow("try");
                        spec.addStatement("__session.startTransaction()");

                        spec.addStatement(
                                "var __find = this.collection.find(__session, $L)$L$L",
                                filter,
                                createSort(context),
                                createProjection(context, true));

                        spec.addStatement("var __toDelete = new $T<$T>()", ArrayList.class, Bson.class);
                        spec.beginControlFlow("for (var __found : __find)");
                        spec.addStatement(
                                "__toDelete.add($T.and($L))",
                                Filters.class,
                                entityInfo.keyColumns().stream()
                                        .map(key ->
                                                "Filters.eq(\"%s\", __found.%s())".formatted(key.name(), key.name()))
                                        .collect(Collectors.joining(", ")));
                        spec.endControlFlow();

                        spec.addStatement(
                                "var __deletedCount = this.collection.deleteMany(__session, $T.or(__toDelete)).getDeletedCount()",
                                Filters.class);

                        spec.beginControlFlow("if (__find.size() != __deletedCount)");
                        spec.addStatement(
                                "throw new $T($S.formatted(__find.size(), __deletedCount))",
                                IllegalStateException.class,
                                "Found %s documents but deleted %s documents");
                        spec.endControlFlow();

                        spec.addStatement("__session.commitTransaction()");

                        if (needsUpdatedCount) {
                            if (context.typeUtils().isType(Boolean.class, context.returnType())) {
                                spec.addStatement("return __deletedCount > 0");
                            } else {
                                spec.addStatement("return (int) __deletedCount"); // todo make more flexible
                            }
                        } else {
                            spec.addStatement("return __find");
                        }

                        spec.nextControlFlow("catch ($T __exception)", Exception.class);
                        spec.addStatement("__session.abortTransaction()");
                        spec.addStatement("throw __exception");
                        spec.nextControlFlow("finally");
                        spec.addStatement("__session.close()");
                        spec.endControlFlow();
                        return;
                    }
                    // todo technically it can be a deleteOne if the factors contain all the key columns
                    spec.addStatement("this.collection.deleteMany($L)", filter);
                }
            }

            if (context.returnInfo().async() && !needsUpdatedCount) {
                spec.addStatement("return null");
                return;
            }

            if (context.typeUtils().isType(Integer.class, context.returnType())) {
                spec.addStatement("return __count");
            } else if (context.typeUtils().isType(Boolean.class, context.returnType())) {
                spec.addStatement("return __count > 0");
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
            } else if (keyword instanceof NullKeyword) {
                // Filters.eq null is true when the value is null or missing, which is desired I think
                builder.add("$T.eq($S, null)", Filters.class, variable.columnName());
            } else if (keyword instanceof NotNullKeyword) {
                // Same as NullKeyword but then 'not'. So that they can be either missing or null
                builder.add("$T.not($T.eq($S, null))", Filters.class, Filters.class, variable.columnName());
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

        // todo and + or support more than 2. So e.g. ByAAndBAndC could be and(a, b, c) instead of and(a, and(b, c))
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
        return createProjection(context, false);
    }

    private CodeBlock createProjection(QueryContext context, boolean needsCollection) {
        var builder = CodeBlock.builder();

        if (context.projection() != null) {
            for (var projection : context.projection().nonSpecialProjectionKeywords()) {
                if (projection instanceof TopProjectionKeyword keyword) {
                    builder.add(".limit($L)", keyword.limit());
                    continue;
                }
                if (projection instanceof SkipProjectionKeyword keyword) {
                    builder.add(".skip($L)", keyword.offset());
                    continue;
                }
                // todo are there other situations I'm missing?
                if (projection instanceof FirstProjectionKeyword && needsCollection) {
                    builder.add(".limit(1)");
                    continue;
                }
                throw new InvalidRepositoryException("Unsupported projection %s", projection.name());
            }

            var columnName = context.projection().columnName();
            if (columnName != null) {
                builder.add(".map($T::$L)", context.entityInfo().asType(), columnName);
            }
        }

        if (context.returnInfo().isCollection()) {
            builder.add(".into(new $T<>())", context.typeUtils().collectionImplementationFor(context.returnType()));
        } else if (needsCollection) {
            builder.add(".into(new $T<>())", ArrayList.class);
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

    /*
     * Creates a document with every column appended. The key is the column name as string and the value is the column
     * name as parameter/variable
     */
    private CodeBlock createDocument(List<ColumnInfo> columns) {
        var builder = CodeBlock.builder().add("new $T()", Document.class);
        for (ColumnInfo column : columns) {
            builder.add(".append($S, $L)", column.name(), column.name());
        }
        return builder.build();
    }
}
