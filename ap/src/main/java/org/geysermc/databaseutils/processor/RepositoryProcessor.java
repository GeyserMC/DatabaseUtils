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

import com.google.auto.common.MoreTypes;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import org.geysermc.databaseutils.IRepository;
import org.geysermc.databaseutils.meta.Repository;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.query.QueryInfo;
import org.geysermc.databaseutils.processor.query.QuerySection;
import org.geysermc.databaseutils.processor.query.VariableSection;
import org.geysermc.databaseutils.processor.type.RepositoryGenerator;
import org.geysermc.databaseutils.processor.util.InvalidRepositoryException;
import org.geysermc.databaseutils.processor.util.TypeUtils;

@AutoService(Processor.class)
public final class RepositoryProcessor extends AbstractProcessor {
    private EntityManager entityManager;
    private Filer filer;
    private Types typeUtils;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.entityManager = new EntityManager(processingEnv.getTypeUtils());
        this.filer = processingEnv.getFiler();
        this.typeUtils = processingEnv.getTypeUtils();
        this.messager = processingEnv.getMessager();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Repository.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        if (env.processingOver()) {
            return false;
        }

        List<List<RepositoryGenerator>> results = new ArrayList<>();
        for (int i = 0; i < RegisteredGenerators.generatorCount(); i++) {
            results.add(new ArrayList<>());
        }
        boolean errorOccurred = false;

        // generate repositories
        for (Element element : env.getElementsAnnotatedWith(Repository.class)) {
            if (element.getKind() != ElementKind.INTERFACE) {
                error("Repositories can only be interfaces!");
                continue;
            }

            try {
                var result = processRepository((TypeElement) element);
                // repository -> database to database -> repository
                for (int i = 0; i < result.size(); i++) {
                    // todo convert this so that finish can be finish(databaseClass) because we need the db type
                    results.get(i).add(result.get(i));
                }
            } catch (InvalidRepositoryException exception) {
                error(exception.getMessage());
                errorOccurred = true;
            }
        }

        if (errorOccurred) {
            return false;
        }

        boolean hasItems = false;
        for (var result : results) {
            hasItems |= !result.isEmpty();
        }
        if (!hasItems) {
            return false;
        }

        List<TypeSpec> generatedTypes = new ArrayList<>();

        // generate databases
        var databases = RegisteredGenerators.databaseGenerators();
        for (int i = 0; i < databases.size(); i++) {
            var generator = databases.get(i);

            var repositoryClasses = new ArrayList<String>();
            boolean hasAsync = false;
            for (var result : results.get(i)) {
                hasAsync |= result.hasAsync();

                var build = result.finish(generator.databaseClass()).build();
                generatedTypes.add(build);
                repositoryClasses.add(build.name);
            }

            var spec = TypeSpec.classBuilder(generator.databaseClass().getSimpleName() + "Generated");
            generator.init(spec, hasAsync);
            generator.addRepositories(repositoryClasses);

            generatedTypes.add(spec.build());
        }

        writeGeneratedTypes(generatedTypes);

        return false;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    void writeGeneratedTypes(List<TypeSpec> specs) {
        for (TypeSpec spec : specs) {
            System.out.println("writing: " + spec);
            try {
                JavaFile.builder("org.geysermc.databaseutils.generated", spec)
                        .build()
                        .writeTo(filer);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    List<RepositoryGenerator> processRepository(TypeElement repository) {
        TypeMirror entityType = null;
        for (TypeMirror mirror : repository.getInterfaces()) {
            if (TypeUtils.isTypeOf(IRepository.class, MoreTypes.asTypeElement(mirror))) {
                entityType = MoreTypes.asDeclared(mirror).getTypeArguments().get(0);
            }
        }

        if (entityType == null) {
            throw new InvalidRepositoryException("Repository has to extend IRepository<EntityClass>");
        }

        var entity = entityManager.processEntity(MoreTypes.asTypeElement(entityType));

        var generators = RegisteredGenerators.repositoryGenerators();
        for (var generator : generators) {
            generator.init(repository.getSimpleName() + "Impl", repository);
        }

        for (Element enclosedElement : repository.getEnclosedElements()) {
            if (enclosedElement.getKind() != ElementKind.METHOD) {
                continue;
            }
            var element = (ExecutableElement) enclosedElement;
            if (element.isDefault()) {
                continue;
            }

            TypeElement returnType;
            boolean async = false;
            if (MoreTypes.isTypeOf(CompletableFuture.class, element.getReturnType())) {
                async = true;
                returnType = TypeUtils.toBoxedTypeElement(
                        MoreTypes.asDeclared(element.getReturnType())
                                .getTypeArguments()
                                .get(0),
                        typeUtils);
            } else {
                returnType = TypeUtils.toBoxedTypeElement(element.getReturnType(), typeUtils);
            }

            var name = element.getSimpleName().toString();

            if (name.startsWith("findBy")) {
                if (!returnType.getQualifiedName().contentEquals(entity.className())) {
                    throw new InvalidRepositoryException(
                            "Expected %s as return type for %s, got %s",
                            entity.className(), element.getSimpleName(), returnType);
                }

                var actions = findActionsFor("findBy", name.substring("findBy".length()), element, entity);
                var queryInfo = new QueryInfo(
                        entity.name(), entity.className(), entity.columns(), actions, element.getParameters());

                for (RepositoryGenerator generator : generators) {
                    generator.addFindBy(queryInfo, MethodSpec.overriding(element), async);
                }
            } else if (name.startsWith("existsBy")) {
                if (!TypeUtils.isTypeOf(Boolean.class, returnType)) {
                    throw new InvalidRepositoryException(
                            "Expected Boolean as return type for %s, got %s", element.getSimpleName(), returnType);
                }

                var actions = findActionsFor("existsBy", name.substring("existsBy".length()), element, entity);
                var queryInfo = new QueryInfo(
                        entity.name(), entity.className(), entity.columns(), actions, element.getParameters());

                for (RepositoryGenerator generator : generators) {
                    generator.addExistsBy(queryInfo, MethodSpec.overriding(element), async);
                }
            } else {
                throw new InvalidRepositoryException("No available actions for %s", name);
            }
        }

        return generators;
    }

    List<QuerySection> findActionsFor(String actionName, String name, ExecutableElement element, EntityInfo info) {
        if (name.isEmpty()) {
            throw new InvalidRepositoryException("Cannot %s nothing!", actionName);
        }

        var sections = new ArrayList<QuerySection>();
        var parameterCount = 0;
        StringBuilder currentSections = new StringBuilder();
        StringBuilder currentSection = new StringBuilder();

        for (int i = 0; i < name.length(); i++) {
            char current = name.charAt(i);
            if (Character.isUpperCase(current)) {
                // this can be a new section!
                var selector = RegisteredActions.selectorFor(currentSection.toString());
                if (selector != null) {
                    var variableName = currentSections.toString();
                    var column = info.columnFor(variableName);
                    if (column == null) {
                        throw new InvalidRepositoryException(
                                "Cannot find column '%s' in %s", variableName, info.className());
                    }
                    var parameter = element.getParameters().get(parameterCount++);
                    var parameterType = TypeUtils.toBoxedTypeElement(parameter.asType(), typeUtils)
                            .getQualifiedName();
                    if (!parameterType.contentEquals(column.typeName())) {
                        throw new InvalidRepositoryException("Column '%s' type %s doesn't match parameter type %s");
                    }

                    sections.add(new VariableSection(variableName));
                    sections.add(selector);
                    currentSections = new StringBuilder();
                } else {
                    currentSections.append(currentSection);
                }
                currentSection = new StringBuilder();

                // UpperCamelCase -> camelCase
                if (currentSections.isEmpty()) {
                    current = Character.toLowerCase(current);
                }
            }
            currentSection.append(current);
        }

        // cannot have a selector as the last action
        if (currentSection.isEmpty() && currentSections.isEmpty()) {
            throw new InvalidRepositoryException("Cannot end a %s with a selector", actionName);
        }

        // assume everything remaining is a variable
        currentSections.append(currentSection);
        var variableName = currentSections.toString();
        var column = info.columnFor(variableName);
        if (column == null) {
            throw new InvalidRepositoryException("Cannot find column '%s' in %s", variableName, info.className());
        }
        var parameter = element.getParameters().get(parameterCount++);
        var parameterType =
                TypeUtils.toBoxedTypeElement(parameter.asType(), typeUtils).getQualifiedName();
        if (!parameterType.contentEquals(column.typeName())) {
            throw new InvalidRepositoryException("Column '%s' type %s doesn't match parameter type %s");
        }
        sections.add(new VariableSection(variableName));

        if (element.getParameters().size() != parameterCount) {
            throw new InvalidRepositoryException(
                    "Expected %s parameters for %s, got %s",
                    parameterCount,
                    element.getSimpleName(),
                    element.getParameters().size());
        }

        return sections;
    }

    void error(final String message, final Object... arguments) {
        this.messager.printMessage(Diagnostic.Kind.ERROR, String.format(Locale.ROOT, message, arguments));
    }

    record RepositoryResult(TypeSpec.Builder generatedRepository, boolean hasAsync) {}
}
