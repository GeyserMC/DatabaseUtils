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
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.query.section.BySection;
import org.geysermc.databaseutils.processor.query.section.FactorRegistry;
import org.geysermc.databaseutils.processor.query.section.by.InputKeywordRegistry;
import org.geysermc.databaseutils.processor.query.section.factor.Factor;
import org.geysermc.databaseutils.processor.query.section.factor.VariableByFactor;
import org.geysermc.databaseutils.processor.query.section.factor.VariableOrderByFactor;
import org.geysermc.databaseutils.processor.query.section.order.OrderBySection;
import org.geysermc.databaseutils.processor.query.section.order.OrderDirection;
import org.geysermc.databaseutils.processor.util.StringUtils;

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

        String action = null;
        boolean isOrderBy = false;
        boolean hadOrder = false;

        var bySections = new ArrayList<String>();
        var orderBySections = new ArrayList<String>();
        var workingSection = new StringBuilder();

        for (int i = 0; i < name.length(); i++) {
            char current = name.charAt(i);

            if (Character.isUpperCase(current)) {
                var finishedSection = workingSection.toString();
                workingSection = new StringBuilder();
                var shouldAdd = true;

                if (action == null) {
                    action = finishedSection;
                } else {
                    if ("By".equals(finishedSection)) {
                        if (hadOrder) {
                            isOrderBy = true;
                            hadOrder = false;
                            shouldAdd = false;
                            bySections.remove(bySections.size() - 1); // Remove Order
                            if (!orderBySections.isEmpty()) {
                                throw new IllegalStateException("Can only have one OrderBy definition!");
                            }
                        } else if (bySections.isEmpty()) {
                            // usually it starts with By (e.g. findByUsername), so ignore it
                            shouldAdd = false;
                        }
                    } else {
                        hadOrder = "Order".equals(finishedSection);
                    }

                    if (shouldAdd) {
                        if (isOrderBy) {
                            orderBySections.add(finishedSection);
                        } else {
                            bySections.add(finishedSection);
                        }
                    }
                }
            }
            workingSection.append(current);
        }

        if (!workingSection.isEmpty()) {
            var finishedSection = workingSection.toString();
            if (action == null) {
                action = finishedSection;
            } else {
                if (isOrderBy) {
                    orderBySections.add(finishedSection);
                } else {
                    bySections.add(finishedSection);
                }
            }
        }

        return new KeywordsReadResult(action, false, formBySection(bySections), formOrderBySection(orderBySections));
    }

    public BySection formBySection(List<String> sections) {
        if (sections.isEmpty()) {
            return null;
        }
        var variables = new ArrayList<Factor>();
        formBySections(sections, 0, variables);
        return new BySection(variables);
    }

    private void formBySections(List<String> bySections, int offset, List<Factor> factors) {
        var variableMatch = StringUtils.largestMatch(bySections, offset, input -> {
            var variableName = StringUtils.uncapitalize(input);
            return variableNames.contains(variableName) ? variableName : null;
        });

        if (variableMatch == null) {
            var factor = FactorRegistry.factorFor(bySections.get(offset));
            if (factor == null) {
                throw new IllegalStateException(
                        "Expected a variable to match, but none did for " + join(bySections, offset));
            }
            factors.add(factor);
            // After a factor was found, restart the process to follow the correct variable -> keyword format
            formBySections(bySections, offset + 1, factors);
            return;
        }

        // findByUsername = username, no keywords after the variable
        if (variableMatch.offset() + 1 == bySections.size()) {
            factors.add(new VariableByFactor(variableMatch.match()));
            return;
        }

        // findByUsernameIsNotNull = IsNotNull
        var keywordMatch =
                StringUtils.largestMatch(bySections, variableMatch.offset() + 1, InputKeywordRegistry::findByName);
        if (keywordMatch == null) {
            var factor = FactorRegistry.factorFor(bySections.get(variableMatch.offset() + 1));
            if (factor == null) {
                throw new IllegalStateException(
                        "Expected a keyword to match for " + join(bySections, variableMatch.offset()));
            }

            // just like above, if no keyword is provided, assume equals
            factors.add(new VariableByFactor(variableMatch.match()));
            factors.add(factor);

            // We have to assume that after the factor something comes next
            formBySections(bySections, variableMatch.offset() + 2, factors);
            return;
        }

        factors.add(new VariableByFactor(variableMatch.match(), keywordMatch.match()));
        if (keywordMatch.offset() + 1 < bySections.size()) {
            formBySections(bySections, keywordMatch.offset() + 1, factors);
        }
    }

    public OrderBySection formOrderBySection(List<String> sections) {
        if (sections.isEmpty()) {
            return null;
        }
        var variables = new ArrayList<Factor>();
        formOrderBySections(sections, 0, variables);
        return new OrderBySection(variables);
    }

    private void formOrderBySections(List<String> orderBySections, int offset, List<Factor> factors) {
        var variableMatch = StringUtils.largestMatch(orderBySections, offset, input -> {
            var variableName = StringUtils.uncapitalize(input);
            return variableNames.contains(variableName) ? variableName : null;
        });

        if (variableMatch == null) {
            var factor = FactorRegistry.factorFor(orderBySections.get(offset));
            if (factor == null) {
                throw new IllegalStateException(
                        "Expected a variable to match, but none did for " + join(orderBySections, offset));
            }
            factors.add(factor);
            // After a factor was found restart the process to follow the correct variable -> direction format
            formOrderBySections(orderBySections, offset + 1, factors);
            return;
        }

        // use the default direction if none is provided
        if (variableMatch.offset() + 1 == orderBySections.size()) {
            factors.add(new VariableOrderByFactor(variableMatch.match(), OrderDirection.DEFAULT));
            return;
        }

        var directionString = StringUtils.uncapitalize(orderBySections.get(variableMatch.offset() + 1));
        var direction = OrderDirection.byName(directionString);
        if (direction == null) {
            var factor = FactorRegistry.factorFor(orderBySections.get(variableMatch.offset() + 1));
            if (factor == null) {
                throw new IllegalStateException("Unknown order by direction " + directionString);
            }

            // just like above, if no direction is provided, use default
            factors.add(new VariableOrderByFactor(variableMatch.match(), OrderDirection.DEFAULT));
            factors.add(factor);

            // We have to assume that after the factor something comes next
            formOrderBySections(orderBySections, variableMatch.offset() + 2, factors);
            return;
        }

        factors.add(new VariableOrderByFactor(variableMatch.match(), direction));
        // +2 because of the direction
        if (variableMatch.offset() + 2 < orderBySections.size()) {
            formOrderBySections(orderBySections, variableMatch.offset() + 2, factors);
        }
    }
}
