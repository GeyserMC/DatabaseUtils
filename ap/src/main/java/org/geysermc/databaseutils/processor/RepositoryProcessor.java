/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor;

import com.google.auto.common.MoreTypes;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
import javax.tools.Diagnostic;
import org.geysermc.databaseutils.IRepository;
import org.geysermc.databaseutils.meta.Query;
import org.geysermc.databaseutils.meta.Repository;
import org.geysermc.databaseutils.processor.action.ActionRegistry;
import org.geysermc.databaseutils.processor.query.KeywordsReader;
import org.geysermc.databaseutils.processor.query.QueryContextCreator;
import org.geysermc.databaseutils.processor.type.RepositoryGenerator;
import org.geysermc.databaseutils.processor.util.InvalidRepositoryException;
import org.geysermc.databaseutils.processor.util.TypeUtils;

@AutoService(Processor.class)
public final class RepositoryProcessor extends AbstractProcessor {
    private TypeUtils typeUtils;
    private EntityManager entityManager;
    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.typeUtils = new TypeUtils(processingEnv.getTypeUtils(), processingEnv.getElementUtils());
        this.entityManager = new EntityManager(typeUtils);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Repository.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        if (env.processingOver()) {
            return true;
        }

        List<List<RepositoryGenerator>> results = new ArrayList<>();
        for (int i = 0; i < RegisteredGenerators.generatorCount(); i++) {
            results.add(new ArrayList<>());
        }
        boolean errorOccurred = false;

        // generate repositories
        for (Element element : env.getElementsAnnotatedWith(Repository.class)) {
            if (element.getKind() != ElementKind.INTERFACE) {
                error(element, "Repositories can only be interfaces!");
                continue;
            }

            try {
                var result = processRepository((TypeElement) element);
                // repository -> database to database -> repository
                for (int i = 0; i < result.size(); i++) {
                    results.get(i).add(result.get(i));
                }
            } catch (InvalidRepositoryException | IllegalStateException exception) {
                error(element, exception);
                errorOccurred = true;
            }
        }

        if (errorOccurred) {
            return true;
        }

        boolean hasItems = false;
        for (var result : results) {
            hasItems |= !result.isEmpty();
        }
        if (!hasItems) {
            return true;
        }

        List<GeneratedType> generatedTypes = new ArrayList<>();

        // generate databases
        var databases = RegisteredGenerators.databaseGenerators();
        for (int i = 0; i < databases.size(); i++) {
            var generator = databases.get(i);

            var repositoryClasses = new ArrayList<String>();
            boolean hasAsync = false;
            for (var result : results.get(i)) {
                hasAsync |= result.hasAsync();

                var build = result.finish(generator.databaseClass()).build();
                generatedTypes.add(new GeneratedType(result.packageName(), build));
                repositoryClasses.add(result.packageName() + "." + build.name);
            }

            var spec = TypeSpec.classBuilder(generator.databaseCategory().upperCamelCaseName() + "DatabaseGenerated");
            generator.init(spec, hasAsync);
            generator.addEntities(entityManager.processedEntities());
            generator.addRepositories(repositoryClasses);

            generatedTypes.add(new GeneratedType(generator.databaseClass().getPackageName(), spec.build()));
        }

        writeGeneratedTypes(generatedTypes);

        return true;
    }

    record GeneratedType(String packageName, TypeSpec database) {}

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    private void writeGeneratedTypes(List<GeneratedType> generatedTypes) {
        for (var entry : generatedTypes) {
            try {
                JavaFile.builder(entry.packageName(), entry.database()).build().writeTo(filer);
            } catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    private List<RepositoryGenerator> processRepository(TypeElement repository) {
        TypeMirror entityType = null;
        for (TypeMirror mirror : repository.getInterfaces()) {
            if (typeUtils.isType(IRepository.class, mirror)) {
                entityType = MoreTypes.asDeclared(mirror).getTypeArguments().get(0);
            }
        }

        if (entityType == null) {
            throw new InvalidRepositoryException("Repository has to extend IRepository<EntityClass>");
        }

        var entity = entityManager.processEntity(entityType);

        var generators = RegisteredGenerators.repositoryGenerators();
        for (var generator : generators) {
            generator.init(repository, entity);
        }

        for (Element enclosedElement : repository.getEnclosedElements()) {
            if (enclosedElement.getKind() != ElementKind.METHOD) {
                continue;
            }
            var element = (ExecutableElement) enclosedElement;
            if (element.isDefault()) {
                continue;
            }

            var methodName = element.getSimpleName().toString();

            var query = methodName;
            var queryAnnotation = enclosedElement.getAnnotation(Query.class);
            if (queryAnnotation != null) {
                query = queryAnnotation.value();
            }

            var result = new KeywordsReader(query, entity).read();
            var action = ActionRegistry.actionMatching(result);
            if (action == null) {
                throw new InvalidRepositoryException("No available actions for %s", methodName);
            }

            try {
                var queryContext = new QueryContextCreator(action, result, element, entity, typeUtils).create();
                action.addTo(generators, queryContext);
            } catch (Throwable exception) {
                error(element, exception);
            }
        }

        return generators;
    }

    private void error(Element cause, String message, Object... arguments) {
        this.messager.printMessage(Diagnostic.Kind.ERROR, String.format(Locale.ROOT, message, arguments), cause);
    }

    private void error(Element cause, Throwable exception) {
        // trimming down the exception until the first trace of ourselves.
        // This would be either a test class or our RepositoryProcessor.
        // This makes the exception much easier to read
        int lastOwnTrace = 0;
        StringBuilder stackTrace = new StringBuilder(exception.toString()).append('\n');
        for (StackTraceElement traceElement : exception.getStackTrace()) {
            stackTrace.append(traceElement.toString()).append('\n');
            if (traceElement.getClassName().startsWith("org.geysermc.databaseutils")) {
                lastOwnTrace = stackTrace.length() - 1;
            }
        }

        if (lastOwnTrace != stackTrace.length() - 1) {
            stackTrace.delete(lastOwnTrace, stackTrace.length());
        }
        this.messager.printMessage(Diagnostic.Kind.ERROR, stackTrace, cause);
    }
}
