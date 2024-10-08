/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.type;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.concurrent.CompletableFuture;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import org.geysermc.databaseutils.DatabaseCategory;
import org.geysermc.databaseutils.codec.TypeCodec;
import org.geysermc.databaseutils.codec.TypeCodecRegistry;
import org.geysermc.databaseutils.processor.info.ColumnInfo;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.query.QueryContext;
import org.geysermc.databaseutils.processor.util.TypeUtils;

public abstract class RepositoryGenerator {
    private final DatabaseCategory category;

    protected TypeSpec.Builder typeSpec;
    protected boolean hasAsync;
    protected EntityInfo entityInfo;

    private String packageName;
    private String className;

    protected RepositoryGenerator(DatabaseCategory category) {
        this.category = category;
    }

    protected void onConstructorBuilder(MethodSpec.Builder builder) {}

    public abstract void addFind(QueryContext context, MethodSpec.Builder spec);

    public abstract void addExists(QueryContext context, MethodSpec.Builder spec);

    public abstract void addInsert(QueryContext context, MethodSpec.Builder spec);

    public abstract void addUpdate(QueryContext context, MethodSpec.Builder spec);

    public abstract void addDelete(QueryContext context, MethodSpec.Builder spec);

    public void init(TypeElement superType, EntityInfo entityInfo) {
        if (this.typeSpec != null) {
            throw new IllegalStateException("Cannot reinitialize RepositoryGenerator");
        }
        this.packageName = TypeUtils.packageNameFor(superType.getQualifiedName());
        this.className = superType.getSimpleName() + category.upperCamelCaseName() + "Impl";
        this.typeSpec = TypeSpec.classBuilder(className)
                .addSuperinterface(ParameterizedTypeName.get(superType.asType()))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        this.entityInfo = entityInfo;
    }

    public String packageName() {
        return packageName;
    }

    public String className() {
        return className;
    }

    public boolean hasAsync() {
        return hasAsync;
    }

    public TypeSpec.Builder finish(Class<?> databaseClass) {
        typeSpec.addField(databaseClass, "database", Modifier.PRIVATE, Modifier.FINAL);

        var constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(databaseClass, "database")
                .addParameter(TypeCodecRegistry.class, "registry")
                .addStatement("this.database = database");
        onConstructorBuilder(constructor);

        // pre-fetch TypeCodec
        for (ColumnInfo column : entityInfo.columns()) {
            if (!TypeUtils.needsTypeCodec(column.typeName())) {
                continue;
            }
            var fieldType = ParameterizedTypeName.get(ClassName.get(TypeCodec.class), ClassName.get(column.asType()));
            typeSpec.addField(fieldType, "__" + column.name(), Modifier.PRIVATE, Modifier.FINAL);
            constructor.addStatement("this.__$L = registry.requireCodecFor($T.class)", column.name(), column.asType());
        }

        typeSpec.addMethod(constructor.build());
        return typeSpec;
    }

    protected void wrapInCompletableFuture(MethodSpec.Builder builder, boolean async, Runnable content) {
        hasAsync |= async;

        if (async) {
            builder.beginControlFlow("return $T.supplyAsync(() ->", CompletableFuture.class);
        }
        content.run();
        if (async) {
            builder.endControlFlow(", this.database.executorService())");
        }
    }
}
