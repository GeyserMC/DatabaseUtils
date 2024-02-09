package org.geysermc.databaseutils.processor.query.section;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Types;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.util.InvalidRepositoryException;
import org.geysermc.databaseutils.processor.util.TypeUtils;

public final class QuerySectionsReader {
    private final String actionName;
    private final String name;
    private final ExecutableElement element;
    private final EntityInfo info;
    private final Types typeUtils;

    public QuerySectionsReader(
            String actionName,
            String name,
            ExecutableElement element,
            EntityInfo info,
            Types typeUtils
    ) {
        this.actionName = actionName;
        this.name = name;
        this.element = element;
        this.info = info;
        this.typeUtils = typeUtils;
    }

    public List<QuerySection> readBySections() {
        return readSections((currentSections, currentSection, parameterCount) -> {
           var selector = QuerySectionRegistry.selectorFor(currentSection);
           if (selector != null) {
               return new StringToSectionsResult(
                       List.of(createVariable(currentSections, parameterCount++), selector),
                       parameterCount);
           }
           return StringToSectionsResult.empty();
        });
    }

    private List<QuerySection> readSections(StringToSections stringToSections) {
        if (name.isEmpty()) {
            throw new InvalidRepositoryException("Cannot %s nothing!", actionName);
        }

        // - Takes readBySection()'s stringToSections as example. -
        // (By)UniqueIdAndUsername will handled like:
        // U -> no sections for *empty string*
        // UniqueI -> no sections for 'unique'
        // UniqueIdA -> no sections for 'Id'
        // UniqueIdAndU -> section for 'And', adds 'uniqueId' as var and 'And' as and-selector
        // * after loop * assume remaining is var, adds 'username' as var

        var sections = new ArrayList<QuerySection>();
        var parameterCount = 0;
        StringBuilder currentSections = new StringBuilder();
        StringBuilder currentSection = new StringBuilder();

        for (int i = 0; i < name.length(); i++) {
            char current = name.charAt(i);
            if (Character.isUpperCase(current)) {
                // this can be a new section!
                var result = stringToSections.sectionsFor(
                        currentSections.toString(), currentSection.toString(), parameterCount);

                if (!result.sections().isEmpty()) {
                    sections.addAll(result.sections());
                    parameterCount = result.parameterCount();
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
        sections.add(createVariable(currentSections.toString(), parameterCount++));

        if (element.getParameters().size() != parameterCount) {
            throw new InvalidRepositoryException(
                    "Expected %s parameters for %s, got %s",
                    parameterCount,
                    element.getSimpleName(),
                    element.getParameters().size());
        }

        return sections;
    }

    private VariableSection createVariable(String variableName, int parameterCount) {
        var column = info.columnFor(variableName);
        if (column == null) {
            throw new InvalidRepositoryException("Cannot find column '%s' in %s", variableName, info.className());
        }
        var parameter = element.getParameters().get(parameterCount);
        var parameterType = TypeUtils.toBoxedTypeElement(parameter.asType(), typeUtils).getQualifiedName();
        if (!parameterType.contentEquals(column.typeName())) {
            throw new InvalidRepositoryException("Column '%s' type %s doesn't match parameter type %s");
        }
        return new VariableSection(variableName);
    }

    @FunctionalInterface
    private interface StringToSections {
        StringToSectionsResult sectionsFor(String currentSections, String currentSection, int parameterCount);
    }

    private record StringToSectionsResult(List<QuerySection> sections, int parameterCount) {
        private static StringToSectionsResult empty() {
            return new StringToSectionsResult(Collections.emptyList(), -1);
        }
    }
}
