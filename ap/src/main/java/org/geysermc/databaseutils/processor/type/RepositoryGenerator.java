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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.concurrent.CompletableFuture;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import org.geysermc.databaseutils.codec.TypeCodec;
import org.geysermc.databaseutils.codec.TypeCodecRegistry;
import org.geysermc.databaseutils.processor.info.ColumnInfo;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.query.QueryContext;
import org.geysermc.databaseutils.processor.util.TypeUtils;

public abstract class RepositoryGenerator {
    protected TypeSpec.Builder typeSpec;
    protected boolean hasAsync;
    private String packageName;
    private EntityInfo entityInfo;

    protected abstract String upperCamelCaseDatabaseType();

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
        var className = superType.getSimpleName() + upperCamelCaseDatabaseType() + "Impl";
        this.typeSpec = TypeSpec.classBuilder(className)
                .addSuperinterface(ParameterizedTypeName.get(superType.asType()))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        this.entityInfo = entityInfo;
    }

    public String packageName() {
        return packageName;
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
            var typeClassName = ClassName.bestGuess(column.typeName().toString());
            var fieldType = ParameterizedTypeName.get(ClassName.get(TypeCodec.class), typeClassName);
            typeSpec.addField(fieldType, "__" + column.name(), Modifier.PRIVATE, Modifier.FINAL);
            constructor.addStatement("this.__$L = registry.requireCodecFor($T.class)", column.name(), typeClassName);
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
