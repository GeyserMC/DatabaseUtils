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
package org.geysermc.databaseutils.processor.query;

import static org.geysermc.databaseutils.processor.util.CollectionUtils.join;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.query.section.BySection;
import org.geysermc.databaseutils.processor.query.section.FactorRegistry;
import org.geysermc.databaseutils.processor.query.section.OrderBySection;
import org.geysermc.databaseutils.processor.query.section.ProjectionSection;
import org.geysermc.databaseutils.processor.query.section.SectionType;
import org.geysermc.databaseutils.processor.query.section.by.InputKeywordRegistry;
import org.geysermc.databaseutils.processor.query.section.factor.Factor;
import org.geysermc.databaseutils.processor.query.section.factor.ProjectionFactor;
import org.geysermc.databaseutils.processor.query.section.factor.VariableByFactor;
import org.geysermc.databaseutils.processor.query.section.factor.VariableOrderByFactor;
import org.geysermc.databaseutils.processor.query.section.order.OrderDirection;
import org.geysermc.databaseutils.processor.query.section.projection.ProjectionKeyword;
import org.geysermc.databaseutils.processor.query.section.projection.ProjectionKeywordRegistry;
import org.geysermc.databaseutils.processor.util.InvalidRepositoryException;
import org.geysermc.databaseutils.processor.util.StringUtils;
import org.geysermc.databaseutils.processor.util.StringUtils.LargestMatchResult;

public class KeywordsReader {
    private final String name;
    private final List<String> variableNames;

    public KeywordsReader(String name, EntityInfo info) {
        this(
                name,
                info.columns().stream()
                        .map(columnInfo -> columnInfo.name().toString())
                        .toList());
    }

    public KeywordsReader(String name, List<String> variableNames) {
        this.name = name;
        // It has to be a List of Strings and not CharSequences!
        // javax.lang.model.element.Name and java.lang.String can have the same value,
        // but they will not be equal to each
        this.variableNames = variableNames;
    }

    public KeywordsReadResult read() {
        // First split everything into sections (find By Unique Id And Username).
        // We can expect every query to follow the same format: keyword (e.g. find) - By and/or OrderBy
        // after both By and OrderBy comes the first variable, after that we can't know what the structure is.
        // We try to form the biggest variable name that still matches:
        // - uniqueId exists
        // - uniqueIdAnd doesn't exist
        // - uniqueIdAndUsername doesn't exist
        // In this result uniqueId matches, after that we look for something to do with the data.
        // In this example there is nothing, so it'll default to the 'equals' keyword

        var sections = new ArrayList<String>();
        var workingSection = new StringBuilder();

        for (int i = 0; i < name.length(); i++) {
            char current = name.charAt(i);

            if (Character.isUpperCase(current)) {
                sections.add(workingSection.toString());
                workingSection = new StringBuilder();
            }
            workingSection.append(current);
        }

        if (!workingSection.isEmpty()) {
            sections.add(workingSection.toString());
        }

        var action = sections.get(0);

        if (sections.size() == 1) {
            return new KeywordsReadResult(action, null, null, null);
        }

        var builder = KeywordsReadResult.builder();
        var currentContext = new SectionContext(null, 1);
        while ((currentContext = determineSection(sections, currentContext)).type != null) {
            currentContext.offset = formSection(currentContext, sections, builder);
        }

        if (currentContext.offset != sections.size()) {
            throw new InvalidRepositoryException(
                    "Unexpected remaining input: %s. %s sections left",
                    join(sections, currentContext.offset), sections.size() - currentContext.offset);
        }

        return builder.build(action);
    }

    private SectionContext determineSection(List<String> sections, SectionContext currentContext) {
        var offset = currentContext.offset;
        // make sure that 'find' also works
        if (sections.size() == offset) {
            return new SectionContext(null, offset);
        }

        SectionType[] types = SectionType.VALUES;
        // Projection always matches, so try matching backwards
        types:
        for (int typeIndex = types.length - 1; typeIndex >= 0; typeIndex--) {
            SectionType type = types[typeIndex];

            if (offset + type.sections().length > sections.size()) {
                continue;
            }

            for (int sectionIndex = 0; sectionIndex < type.sections().length; sectionIndex++) {
                if (!sections.get(offset + sectionIndex).equals(type.sections()[sectionIndex])) {
                    continue types;
                }
            }

            // make sure you can't redefine sections
            if (SectionType.isCorrectOrder(type, currentContext.type)) {
                return new SectionContext(type, offset + type.sections().length);
            }
        }

        return new SectionContext(null, offset);
    }

    private int formSection(SectionContext context, List<String> sections, KeywordsReadResult.Builder builder) {
        var offset = context.offset;
        if (sections.size() == offset) {
            return offset;
        }

        var factors = new ArrayList<Factor>();
        do {
            offset = formSectionItem(context.type, sections, offset, factors);
        } while (!isNextSection(sections, offset, context.type));

        switch (context.type) {
            case PROJECTION -> builder.projection(ProjectionSection.from(factors));
            case BY -> builder.bySection(new BySection(factors));
            case ORDER_BY -> builder.orderBySection(new OrderBySection(factors));
        }
        return offset;
    }

    public boolean isNextSection(List<String> sections, int offset, SectionType currentSection) {
        if (sections.size() == offset) {
            return true;
        }
        var determinedType = determineSection(sections, new SectionContext(currentSection, offset)).type;
        // projection matches on any var
        return determinedType != null && determinedType != SectionType.PROJECTION;
    }

    private int formSectionItem(SectionType type, List<String> sections, int offset, List<Factor> factors) {
        // every section has a quite specific format:
        // projection: keyword(s) - column name (either of them can be optional, not both)
        // by: column name - optional keyword
        // orderBy: column name - optional direction

        if (type == null) {
            throw new IllegalStateException(String.format(
                    "Expected type to not be null! Remaining: %s. Offset: %s", join(sections, offset), offset));
        }

        if (type == SectionType.PROJECTION) {
            ProjectionKeyword keyword;
            var hadKeyword = false;
            do {
                keyword = ProjectionKeywordRegistry.findByName(sections.get(offset));
                if (keyword != null) {
                    hadKeyword = true;
                    factors.add(new ProjectionFactor(keyword, null));
                    offset++;
                }
            } while (keyword != null);

            var variable = variableMatch(sections, offset);
            if (variable == null) {
                if (hadKeyword) {
                    return offset;
                }
                throw new IllegalStateException(
                        "Expected a projection, got nothing! Remaining: " + join(sections, offset - 1));
            }
            factors.add(new ProjectionFactor(null, variable.match()));
            return variable.offset() + 1;
        }

        var variable = variableMatch(sections, offset);
        if (variable == null) {
            var factor = FactorRegistry.factorFor(sections.get(offset));
            if (factor == null) {
                throw new IllegalStateException(
                        "Expected a variable to match, but none did for " + join(sections, offset));
            }
            factors.add(factor);
            return offset + 1;
        }

        return switch (type) {
            case BY -> createNormal(
                    type, sections, variable, factors, VariableByFactor::new, InputKeywordRegistry::findByName, true);
            case ORDER_BY -> createNormal(
                    type, sections, variable, factors, VariableOrderByFactor::new, OrderDirection::byName, false);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    private <T> int createNormal(
            SectionType type,
            List<String> sections,
            LargestMatchResult<String> variable,
            List<Factor> factors,
            BiFunction<String, T, Factor> creator,
            Function<String, T> matcher,
            boolean largest) {
        // specifying a direction / keyword is optional
        if (variable.offset() + 1 == sections.size() || isNextSection(sections, variable.offset() + 1, type)) {
            factors.add(creator.apply(variable.match(), null));
            return variable.offset() + 1;
        }

        LargestMatchResult<T> keyword;
        if (largest) {
            keyword = StringUtils.largestMatch(sections, variable.offset() + 1, matcher);
        } else {
            var newIndex = variable.offset() + 1;
            var result = matcher.apply(sections.get(newIndex));
            keyword = result == null ? null : new LargestMatchResult<>(result, newIndex);
        }

        if (keyword == null) {
            var factor = FactorRegistry.factorFor(sections.get(variable.offset() + 1));
            if (factor == null) {
                throw new IllegalStateException("Expected a keyword to match for " + join(sections, variable.offset()));
            }

            // just like above, if no keyword is provided, use the default
            factors.add(creator.apply(variable.match(), null));
            factors.add(factor);

            // We have to assume that after the factor something comes next
            return variable.offset() + 2;
        }

        factors.add(creator.apply(variable.match(), keyword.match()));
        return keyword.offset() + 1;
    }

    private LargestMatchResult<String> variableMatch(List<String> sections, int offset) {
        return StringUtils.largestMatch(sections, offset, input -> {
            var variableName = StringUtils.uncapitalize(input);
            return variableNames.contains(variableName) ? variableName : null;
        });
    }

    private static final class SectionContext {
        private final SectionType type;
        private int offset;

        SectionContext(SectionType type, int offset) {
            this.type = type;
            this.offset = offset;
        }
    }
}
