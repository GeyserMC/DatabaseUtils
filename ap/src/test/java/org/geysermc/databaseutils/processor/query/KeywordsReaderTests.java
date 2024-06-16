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

import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.geysermc.databaseutils.processor.query.section.by.keyword.EqualsKeyword;
import org.geysermc.databaseutils.processor.query.section.by.keyword.LessThanKeyword;
import org.geysermc.databaseutils.processor.query.section.factor.AndFactor;
import org.geysermc.databaseutils.processor.query.section.factor.Factor;
import org.geysermc.databaseutils.processor.query.section.factor.OrFactor;
import org.geysermc.databaseutils.processor.query.section.factor.VariableByFactor;
import org.geysermc.databaseutils.processor.query.section.factor.VariableOrderByFactor;
import org.geysermc.databaseutils.processor.query.section.order.OrderDirection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class KeywordsReaderTests {
    @ParameterizedTest
    @MethodSource("okSimpleInputs")
    void okSimple(
            String input, List<String> variables, String action, List<Factor> byFactors, List<Factor> orderByFactors) {
        commonOkLogic(input, variables, action, byFactors, orderByFactors);
    }

    @ParameterizedTest
    @MethodSource("okComplexInputs")
    void okComplex(
            String input, List<String> variables, String action, List<Factor> byFactors, List<Factor> orderByFactors) {
        commonOkLogic(input, variables, action, byFactors, orderByFactors);
    }

    private void commonOkLogic(
            String input, List<String> variables, String action, List<Factor> byFactors, List<Factor> orderByFactors) {
        var result = new KeywordsReader(input, variables).read();
        Assertions.assertEquals(action, result.actionName());

        if (byFactors == null) {
            Assertions.assertNull(result.bySection());
        } else {
            Assertions.assertNotNull(result.bySection());
            var actualVariables = result.bySection().factors();
            Assertions.assertEquals(byFactors.size(), actualVariables.size());

            for (int i = 0; i < byFactors.size(); i++) {
                Assertions.assertEquals(byFactors.get(i), actualVariables.get(i));
            }
        }

        if (orderByFactors == null) {
            Assertions.assertNull(result.orderBySection());
        } else {
            Assertions.assertNotNull(result.orderBySection());
            var actualVariables = result.orderBySection().factors();
            Assertions.assertEquals(orderByFactors.size(), actualVariables.size());

            for (int i = 0; i < orderByFactors.size(); i++) {
                Assertions.assertEquals(orderByFactors.get(i), actualVariables.get(i));
            }
        }
    }

    static Stream<Arguments> okSimpleInputs() {
        return Stream.of(
                arguments("update", Collections.emptyList(), "update", null, null),
                arguments(
                        "updateByUsername",
                        List.of("username"),
                        "update",
                        List.of(new VariableByFactor("username", new EqualsKeyword())),
                        null),
                arguments("find", Collections.emptyList(), "find", null, null),
                arguments(
                        "findByUsername",
                        List.of("username"),
                        "find",
                        List.of(new VariableByFactor("username", new EqualsKeyword())),
                        null),
                arguments(
                        "findByUniqueId",
                        List.of("uniqueId"),
                        "find",
                        List.of(new VariableByFactor("uniqueId", new EqualsKeyword())),
                        null),
                arguments(
                        "findByUsernameLessThan",
                        List.of("username"),
                        "find",
                        List.of(new VariableByFactor("username", new LessThanKeyword())),
                        null),
                arguments(
                        "findByUniqueIdLessThan",
                        List.of("uniqueId"),
                        "find",
                        List.of(new VariableByFactor("uniqueId", new LessThanKeyword())),
                        null),
                arguments(
                        "findOrderByUsername",
                        List.of("username"),
                        "find",
                        null,
                        List.of(new VariableOrderByFactor("username", OrderDirection.DEFAULT))),
                arguments(
                        "findOrderByUsernameAsc",
                        List.of("username"),
                        "find",
                        null,
                        List.of(new VariableOrderByFactor("username", OrderDirection.ASCENDING))),
                arguments(
                        "findByUsernameOrderByUsername",
                        List.of("username"),
                        "find",
                        List.of(new VariableByFactor("username", new EqualsKeyword())),
                        List.of(new VariableOrderByFactor("username", OrderDirection.DEFAULT))),
                arguments(
                        "findByUsernameOrderByUsernameAsc",
                        List.of("username"),
                        "find",
                        List.of(new VariableByFactor("username", new EqualsKeyword())),
                        List.of(new VariableOrderByFactor("username", OrderDirection.ASCENDING))),
                arguments(
                        "findByUsernameOrderByUsernameDesc",
                        List.of("username"),
                        "find",
                        List.of(new VariableByFactor("username", new EqualsKeyword())),
                        List.of(new VariableOrderByFactor("username", OrderDirection.DESCENDING))),
                arguments(
                        "findByUniqueIdOrderByUsername",
                        List.of("uniqueId", "username"),
                        "find",
                        List.of(new VariableByFactor("uniqueId", new EqualsKeyword())),
                        List.of(new VariableOrderByFactor("username", OrderDirection.DEFAULT))),
                arguments(
                        "findByUsernameLessThanOrderByUniqueId",
                        List.of("username", "uniqueId"),
                        "find",
                        List.of(new VariableByFactor("username", new LessThanKeyword())),
                        List.of(new VariableOrderByFactor("uniqueId", OrderDirection.DEFAULT))),
                arguments(
                        "findByUniqueIdLessThanOrderByUniqueId",
                        List.of("uniqueId"),
                        "find",
                        List.of(new VariableByFactor("uniqueId", new LessThanKeyword())),
                        List.of(new VariableOrderByFactor("uniqueId", OrderDirection.DEFAULT))));
    }

    static Stream<Arguments> okComplexInputs() {
        return Stream.of(
                arguments(
                        "findByUsernameAndPassword",
                        List.of("username", "password"),
                        "find",
                        List.of(
                                new VariableByFactor("username", new EqualsKeyword()),
                                AndFactor.INSTANCE,
                                new VariableByFactor("password", new EqualsKeyword())),
                        null),
                arguments(
                        "findByUniqueIdAndPassword",
                        List.of("uniqueId", "password"),
                        "find",
                        List.of(
                                new VariableByFactor("uniqueId", new EqualsKeyword()),
                                AndFactor.INSTANCE,
                                new VariableByFactor("password", new EqualsKeyword())),
                        null),
                arguments(
                        "findByUniqueIdAndUniqueName",
                        List.of("uniqueId", "uniqueName"),
                        "find",
                        List.of(
                                new VariableByFactor("uniqueId", new EqualsKeyword()),
                                AndFactor.INSTANCE,
                                new VariableByFactor("uniqueName", new EqualsKeyword())),
                        null),
                arguments(
                        "findByUsernameLessThanAndPassword",
                        List.of("username", "password"),
                        "find",
                        List.of(
                                new VariableByFactor("username", new LessThanKeyword()),
                                AndFactor.INSTANCE,
                                new VariableByFactor("password", new EqualsKeyword())),
                        null),
                arguments(
                        "findByUniqueIdLessThanOrMyHash",
                        List.of("uniqueId", "myHash"),
                        "find",
                        List.of(
                                new VariableByFactor("uniqueId", new LessThanKeyword()),
                                OrFactor.INSTANCE,
                                new VariableByFactor("myHash", new EqualsKeyword())),
                        null),
                arguments(
                        "findOrderByUsernameOrEmail",
                        List.of("username", "email"),
                        "find",
                        null,
                        List.of(
                                new VariableOrderByFactor("username", OrderDirection.DEFAULT),
                                OrFactor.INSTANCE,
                                new VariableOrderByFactor("email", OrderDirection.DEFAULT))),
                arguments(
                        "findOrderByUsernameAscOrPassword",
                        List.of("username", "password"),
                        "find",
                        null,
                        List.of(
                                new VariableOrderByFactor("username", OrderDirection.ASCENDING),
                                OrFactor.INSTANCE,
                                new VariableOrderByFactor("password", OrderDirection.DEFAULT))),
                arguments(
                        "findByUsernameOrderByUsernameAndPassword",
                        List.of("username", "password"),
                        "find",
                        List.of(new VariableByFactor("username", new EqualsKeyword())),
                        List.of(
                                new VariableOrderByFactor("username", OrderDirection.DEFAULT),
                                AndFactor.INSTANCE,
                                new VariableOrderByFactor("password", OrderDirection.DEFAULT))),
                arguments(
                        "findByUsernameOrderByUsernameAscOrPasswordDesc",
                        List.of("username", "password"),
                        "find",
                        List.of(new VariableByFactor("username", new EqualsKeyword())),
                        List.of(
                                new VariableOrderByFactor("username", OrderDirection.ASCENDING),
                                OrFactor.INSTANCE,
                                new VariableOrderByFactor("password", OrderDirection.DESCENDING))),
                arguments(
                        "findByUniqueIdLessThanOrUsernameOrderByUniqueIdAndPingDesc",
                        List.of("uniqueId", "username", "ping"),
                        "find",
                        List.of(
                                new VariableByFactor("uniqueId", new LessThanKeyword()),
                                OrFactor.INSTANCE,
                                new VariableByFactor("username", new EqualsKeyword())),
                        List.of(
                                new VariableOrderByFactor("uniqueId", OrderDirection.DEFAULT),
                                AndFactor.INSTANCE,
                                new VariableOrderByFactor("ping", OrderDirection.DESCENDING))));
    }
}
