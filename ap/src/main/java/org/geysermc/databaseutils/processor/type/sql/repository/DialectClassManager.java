/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.type.sql.repository;

import static org.geysermc.databaseutils.processor.util.CollectionUtils.join;
import static org.geysermc.databaseutils.processor.util.StringUtils.screamingSnakeCaseToPascalCase;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeVariable;
import org.geysermc.databaseutils.processor.query.QueryContext;
import org.geysermc.databaseutils.sql.SqlDialect;

public class DialectClassManager {
    private final List<CreateManager> managers = new ArrayList<>();
    private final TypeSpec.Builder typeSpec;
    private final String className;

    private Map<SqlDialect, TypeSpec> cachedFinish;
    private List<TypeSpec> cachedTypes;

    public DialectClassManager(TypeSpec.Builder typeSpec, String className) {
        this.typeSpec = typeSpec;
        this.className = className;
    }

    public CreateManager create(QueryContext context, MethodSpec.Builder builder) {
        if (context.returnInfo().isVoid()) {
            builder.addStatement(
                    "this.dialectSpecific.$L($L)",
                    context.methodName(),
                    join(context.parametersInfo().names()));
        } else {
            builder.addStatement(
                    "return this.dialectSpecific.$L($L)",
                    context.methodName(),
                    join(context.parametersInfo().names()));
        }
        typeSpec.addMethod(builder.build());

        var manager = new CreateManager(context, className);
        managers.add(manager);
        return manager;
    }

    public void onConstructorBuilder(MethodSpec.Builder builder) {
        if (managers.isEmpty()) {
            return;
        }
        finish();

        typeSpec.addField(ClassName.get("", "CommonImpl"), "dialectSpecific", Modifier.PRIVATE, Modifier.FINAL);

        AtomicBoolean first = new AtomicBoolean(true);
        cachedFinish.forEach((dialect, type) -> {
            if (dialect == null) {
                return;
            }
            if (first.getAndSet(false)) {
                builder.beginControlFlow("if (this.dialect == $T.$L)", SqlDialect.class, dialect);
            } else {
                builder.nextControlFlow("else if (this.dialect == $T.$L)", SqlDialect.class, dialect);
            }
            builder.addStatement("this.dialectSpecific = new $T()", ClassName.get("", type.name));
        });
        builder.nextControlFlow("else");
        builder.addStatement("this.dialectSpecific = new $T()", ClassName.get("", "CommonImpl"));
        builder.endControlFlow();
    }

    public List<TypeSpec> finish() {
        if (cachedFinish != null) {
            return Collections.unmodifiableList(cachedTypes);
        }

        var createdSpecs = new LinkedHashMap<SqlDialect, TypeSpec.Builder>();
        for (CreateManager manager : managers) {
            manager.validate();
            manager.createdMethods.forEach((dialect, value) -> {
                var type = createdSpecs.computeIfAbsent(dialect, k -> {
                    var dialectName = dialect == null ? "Common" : screamingSnakeCaseToPascalCase(dialect.name());
                    var builder = TypeSpec.classBuilder(dialectName + "Impl");
                    builder.addModifiers(Modifier.PRIVATE);
                    if (dialect != null) {
                        builder.superclass(ClassName.get("", "CommonImpl"));
                        builder.addModifiers(Modifier.FINAL);
                    }
                    return builder;
                });
                type.addMethod(value);
            });
        }

        this.cachedFinish = new LinkedHashMap<>();
        this.cachedTypes = new ArrayList<>();
        createdSpecs.forEach((key, value) -> {
            var typeSpec = value.build();
            this.cachedFinish.put(key, typeSpec);
            this.cachedTypes.add(typeSpec);
        });
        return Collections.unmodifiableList(cachedTypes);
    }

    public static final class CreateManager {
        private final QueryContext context;
        private final String className;
        private final Map<SqlDialect, MethodSpec> createdMethods = new LinkedHashMap<>();

        private CreateManager(QueryContext context, String className) {
            this.context = context;
            this.className = className;
        }

        public void createDefault(Consumer<DialectMethod> consumer) {
            create((SqlDialect) null, consumer);
        }

        public void create(SqlDialect dialect, Consumer<DialectMethod> consumer) {
            create(Collections.singletonList(dialect), consumer);
        }

        public void create(List<SqlDialect> dialects, Consumer<DialectMethod> consumer) {
            var methodBuilder = MethodSpec.methodBuilder(context.methodName().toString());
            // MethodSpec#overriding with a few tweaks
            var method = context.method();
            for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
                TypeVariable var = (TypeVariable) typeParameterElement.asType();
                methodBuilder.addTypeVariable(TypeVariableName.get(var));
            }
            methodBuilder.returns(TypeName.get(method.getReturnType()));
            for (VariableElement parameter : method.getParameters()) {
                methodBuilder.addParameter(ParameterSpec.get(parameter));
            }
            methodBuilder.varargs(method.isVarArgs());

            var dialectMethod = new DialectMethod(methodBuilder, className);
            consumer.accept(dialectMethod);
            var commonBuild = dialectMethod.build();
            for (SqlDialect dialect : dialects) {
                var build = commonBuild;
                if (dialect != null) {
                    build = build.toBuilder().addAnnotation(Override.class).build();
                }
                createdMethods.put(dialect, build);
            }
        }

        public void validate() {
            if (createdMethods.get(null) == null) {
                throw new IllegalStateException("No base implementation found!");
            }
        }
    }
}
