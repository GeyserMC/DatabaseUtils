/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.type.mongo;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.ArrayList;
import java.util.Collection;
import javax.lang.model.element.Modifier;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.geysermc.databaseutils.DatabaseCategory;
import org.geysermc.databaseutils.meta.Index;
import org.geysermc.databaseutils.mongo.MongodbDatabase;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.info.IndexInfo;
import org.geysermc.databaseutils.processor.type.DatabaseGenerator;

public class MongoDatabaseGenerator extends DatabaseGenerator {
    public MongoDatabaseGenerator() {
        super(DatabaseCategory.MONGODB);
    }

    @Override
    public Class<?> databaseClass() {
        return MongodbDatabase.class;
    }

    @Override
    protected void addEntities(Collection<EntityInfo> entities, MethodSpec.Builder method) {
        method.addException(MongoException.class);
        method.addStatement("$T mongoDatabase = database.mongoDatabase()", MongoDatabase.class);

        method.addStatement(
                "var collectionNames = mongoDatabase.listCollectionNames().into(new $T<>())", ArrayList.class);

        // todo add entity name deduplication
        // method.addStatement("$T<$T> collection = mongoDatabase.getCollection($S)", MongoCollection.class,
        // entity.type(), entity.name());
        for (EntityInfo entity : entities) {
            createEntityQuery(entity, method);
        }

        addEntityCodecs(entities, this.spec);
    }

    private void createEntityQuery(EntityInfo entity, MethodSpec.Builder method) {
        method.beginControlFlow("if (!collectionNames.contains($S))", entity.name());

        method.addStatement("mongoDatabase.createCollection($S)", entity.name());
        method.addStatement(
                "$T<$T> collection = mongoDatabase.getCollection($S)",
                MongoCollection.class,
                Document.class,
                entity.name());

        entity.indexes().forEach(index -> method.addStatement("collection.createIndex($L)", createIndex(index)));

        method.endControlFlow();
    }

    private CodeBlock createIndex(IndexInfo info) {
        var builder = CodeBlock.builder();
        builder.add("new $T()", Document.class);

        for (CharSequence column : info.columns()) {
            builder.add(".append($S, $L)", column, indexDirection(info.direction()));
        }

        if (info.unique()) {
            builder.add(", new $T().unique(true)", IndexOptions.class);
        }
        return builder.build();
    }

    private int indexDirection(Index.IndexDirection direction) {
        return switch (direction) {
            case ASCENDING -> 1;
            case DESCENDING -> -1;
        };
    }

    private void addEntityCodecs(Collection<EntityInfo> entities, TypeSpec.Builder spec) {
        var entityCodecSpec = TypeSpec.classBuilder("EntityCodecProvider")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .addSuperinterface(CodecProvider.class);

        var getMethod = MethodSpec.methodBuilder("get")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", "{$S}", "unchecked")
                        .build())
                .addTypeVariable(TypeVariableName.get("T"))
                .returns(ParameterizedTypeName.get(ClassName.get(Codec.class), TypeVariableName.get("T")))
                .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), TypeVariableName.get("T")), "clazz")
                .addParameter(CodecRegistry.class, "registry");

        for (EntityInfo entity : entities) {
            var typeName = EntityCodecGenerator.createFor(entity, spec);
            getMethod.beginControlFlow("if (clazz == $T.class)", entity.type());
            getMethod.addStatement("return ($T<T>) new $L(registry)", Codec.class, typeName);
            getMethod.endControlFlow();
        }

        getMethod.addStatement("return null");

        entityCodecSpec.addMethod(getMethod.build());
        spec.addType(entityCodecSpec.build());

        spec.addField(FieldSpec.builder(CodecProvider.class, "ENTITY_CODECS")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $L()", "EntityCodecProvider")
                .build());
    }
}
