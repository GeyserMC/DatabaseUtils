/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.type;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import javax.lang.model.element.Modifier;
import org.geysermc.databaseutils.DatabaseCategory;
import org.geysermc.databaseutils.IRepository;
import org.geysermc.databaseutils.codec.TypeCodecRegistry;
import org.geysermc.databaseutils.processor.info.EntityInfo;

public abstract class DatabaseGenerator {
    private final DatabaseCategory databaseCategory;
    protected TypeSpec.Builder spec;

    public DatabaseGenerator(DatabaseCategory databaseCategory) {
        this.databaseCategory = databaseCategory;
    }

    public void init(TypeSpec.Builder spec, boolean hasAsync) {
        if (this.spec != null) {
            throw new IllegalStateException("Cannot reinitialize RepositoryGenerator");
        }
        this.spec = spec;
        spec.addField(FieldSpec.builder(Boolean.TYPE, "HAS_ASYNC", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$L", hasAsync)
                .build());
    }

    public DatabaseCategory databaseCategory() {
        return databaseCategory;
    }

    public abstract Class<?> databaseClass();

    protected abstract void addEntities(Collection<EntityInfo> entities, MethodSpec.Builder method);

    public void addEntities(Collection<EntityInfo> entities) {
        var builder = MethodSpec.methodBuilder("createEntities")
                .addModifiers(Modifier.STATIC)
                .addParameter(databaseClass(), "database");
        addEntities(entities, builder);
        spec.addMethod(builder.build());
    }

    public void addRepositories(List<String> repositoriesClassName) {
        var builder = CodeBlock.builder().addStatement("REPOSITORIES = new $T<>()", ArrayList.class);
        for (String repository : repositoriesClassName) {
            builder.addStatement("REPOSITORIES.add($T::new)", ClassName.bestGuess(repository));
        }
        spec.addStaticBlock(builder.build());

        // List<BiFunction<dbClass, TypeCodecRegistry, IRepository<?>>>
        spec.addField(
                ParameterizedTypeName.get(
                        ClassName.get(List.class),
                        ParameterizedTypeName.get(
                                ClassName.get(BiFunction.class),
                                ClassName.get(databaseClass()),
                                ClassName.get(TypeCodecRegistry.class),
                                ParameterizedTypeName.get(
                                        ClassName.get(IRepository.class), WildcardTypeName.subtypeOf(Object.class)))),
                "REPOSITORIES",
                Modifier.PRIVATE,
                Modifier.STATIC,
                Modifier.FINAL);
    }
}
