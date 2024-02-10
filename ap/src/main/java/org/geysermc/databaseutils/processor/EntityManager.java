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
package org.geysermc.databaseutils.processor;

import static org.geysermc.databaseutils.processor.util.AnnotationUtils.hasAnnotation;
import static org.geysermc.databaseutils.processor.util.TypeUtils.toBoxedTypeElement;

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
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Types;
import org.geysermc.databaseutils.meta.Entity;
import org.geysermc.databaseutils.meta.Key;
import org.geysermc.databaseutils.processor.info.ColumnInfo;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.info.IndexInfo;

final class EntityManager {
    private final Map<CharSequence, EntityInfo> entityInfoByClassName = new HashMap<>();
    private final Types typeUtils;

    EntityManager(final Types typeUtils) {
        this.typeUtils = Objects.requireNonNull(typeUtils);
    }

    Collection<EntityInfo> processedEntities() {
        return entityInfoByClassName.values();
    }

    EntityInfo processEntity(TypeElement type) {
        var cached = entityInfoByClassName.get(type.getQualifiedName());
        if (cached != null) {
            return cached;
        }

        Entity entity = type.getAnnotation(Entity.class);
        if (entity == null) {
            throw new IllegalStateException("Tried to process entity without an Entity annotation");
        }
        String tableName = entity.value();

        var constructors = new ArrayList<ExecutableElement>();
        var keys = new ArrayList<CharSequence>();

        var indexes = new ArrayList<IndexInfo>();
        var columns = new ArrayList<ColumnInfo>();

        Arrays.stream(type.getAnnotationsByType(org.geysermc.databaseutils.meta.Index.class))
                .map(index -> new IndexInfo(index.name(), index.columns(), index.unique()))
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

            TypeElement typeElement = toBoxedTypeElement(field.asType(), typeUtils);
            columns.add(new ColumnInfo(field.getSimpleName(), typeElement.getQualifiedName()));

            if (hasAnnotation(field, Key.class)) {
                keys.add(field.getSimpleName());
            }
            var index = field.getAnnotation(org.geysermc.databaseutils.meta.Index.class);
            if (index != null) {
                indexes.add(new IndexInfo(index.name(), index.columns(), index.unique()));
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
                var parameterType = toBoxedTypeElement(parameters.get(i).asType(), typeUtils)
                        .getQualifiedName();
                if (!columns.get(i).typeName().equals(parameterType)) {
                    continue constructors;
                }
            }
            validConstructorFound = true;
            break;
        }

        if (!validConstructorFound) {
            throw new IllegalStateException("No valid all-arg constructor found or invalid parameter order");
        }

        if (!keys.isEmpty()) {
            indexes.add(new IndexInfo("", keys.toArray(new CharSequence[0]), true));
        }

        var entityInfo = new EntityInfo(tableName, type.getQualifiedName(), columns, indexes, keys);
        entityInfoByClassName.put(type.getQualifiedName(), entityInfo);
        return entityInfo;
    }
}
