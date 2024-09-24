/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor;

import static org.geysermc.databaseutils.processor.util.AnnotationUtils.hasAnnotation;

import com.google.auto.common.MoreTypes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.geysermc.databaseutils.meta.Entity;
import org.geysermc.databaseutils.meta.Index;
import org.geysermc.databaseutils.meta.Key;
import org.geysermc.databaseutils.processor.info.ColumnInfo;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.info.IndexInfo;
import org.geysermc.databaseutils.processor.info.IndexInfo.IndexType;
import org.geysermc.databaseutils.processor.type.LimitsEnforcer;
import org.geysermc.databaseutils.processor.util.TypeUtils;

final class EntityManager {
    private final Map<CharSequence, EntityInfo> entityInfoByClassName = new HashMap<>();
    private final TypeUtils typeUtils;

    EntityManager(final TypeUtils typeUtils) {
        this.typeUtils = Objects.requireNonNull(typeUtils);
    }

    Collection<EntityInfo> processedEntities() {
        return entityInfoByClassName.values();
    }

    EntityInfo processEntity(TypeMirror typeMirror) {
        // todo technically all those meta annotation classes can be moved a separate module you only need during
        // compile time, instead of including the annotation definitions in runtime as well

        var type = MoreTypes.asTypeElement(typeMirror);

        var cached = entityInfoByClassName.get(type.getQualifiedName());
        if (cached != null) {
            return cached;
        }

        Entity entity = type.getAnnotation(Entity.class);
        if (entity == null) {
            throw new IllegalStateException("Tried to process entity without an Entity annotation");
        }
        String tableName = entity.value();
        if ("".equals(tableName)) {
            tableName = type.getSimpleName().toString();
        }

        var constructors = new ArrayList<ExecutableElement>();
        var keys = new ArrayList<CharSequence>();

        var indexes = new ArrayList<IndexInfo>();
        var columns = new ArrayList<ColumnInfo>();

        Arrays.stream(type.getAnnotationsByType(Index.class))
                .map(index -> new IndexInfo(index.name(), index.columns(), index.unique(), index.direction()))
                .forEach(indexes::add);

        for (Element element : type.getEnclosedElements()) {
            if (element.getKind() == ElementKind.CONSTRUCTOR) {
                var constructor = (ExecutableElement) element;
                if (!constructor.getParameters().isEmpty()) {
                    constructors.add(constructor);
                }
                continue;
            }
            if (element.getKind() != ElementKind.FIELD) {
                continue;
            }

            var field = (VariableElement) element;
            if (field.getModifiers().contains(Modifier.STATIC)) {
                continue;
            }

            columns.add(new ColumnInfo(field.getSimpleName(), typeUtils.toBoxedTypeElement(field.asType()), field));

            if (hasAnnotation(field, Key.class)) {
                keys.add(field.getSimpleName());
            }
            var index = field.getAnnotation(Index.class);
            if (index != null) {
                indexes.add(new IndexInfo(index.name(), index.columns(), index.unique(), index.direction()));
            }
        }

        boolean validConstructorFound = false;

        constructors:
        for (ExecutableElement element : constructors) {
            var parameters = element.getParameters();
            if (parameters.size() != columns.size()) {
                continue;
            }

            for (int i = 0; i < parameters.size(); i++) {
                if (!typeUtils.isType(columns.get(i).asType(), parameters.get(i).asType())) {
                    continue constructors;
                }
            }
            validConstructorFound = true;
            break;
        }

        if (!validConstructorFound) {
            throw new IllegalStateException(
                    "No valid all-arg constructor found or invalid parameter order for %s".formatted(typeMirror));
        }

        if (!keys.isEmpty()) {
            indexes.add(new IndexInfo("", keys.toArray(new CharSequence[0]), IndexType.PRIMARY));
        } else {
            // todo just make every column a key
            throw new IllegalStateException("Expected entity to have at least one field marked as key");
        }

        var entityInfo = new EntityInfo(tableName, type, columns, indexes, keys);

        new LimitsEnforcer(entityInfo).enforce();

        entityInfoByClassName.put(type.getQualifiedName(), entityInfo);
        return entityInfo;
    }
}
