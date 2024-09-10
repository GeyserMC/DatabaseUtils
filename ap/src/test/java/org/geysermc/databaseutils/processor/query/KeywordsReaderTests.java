/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.query;

import static org.geysermc.databaseutils.processor.util.CollectionUtils.join;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.geysermc.databaseutils.processor.query.section.by.keyword.EqualsKeyword;
import org.geysermc.databaseutils.processor.query.section.by.keyword.LessThanKeyword;
import org.geysermc.databaseutils.processor.query.section.factor.AndFactor;
import org.geysermc.databaseutils.processor.query.section.factor.Factor;
import org.geysermc.databaseutils.processor.query.section.factor.OrFactor;
import org.geysermc.databaseutils.processor.query.section.factor.ProjectionFactor;
import org.geysermc.databaseutils.processor.query.section.factor.VariableByFactor;
import org.geysermc.databaseutils.processor.query.section.factor.VariableOrderByFactor;
import org.geysermc.databaseutils.processor.query.section.order.OrderDirection;
import org.geysermc.databaseutils.processor.query.section.projection.keyword.DistinctProjectionKeyword;
import org.geysermc.databaseutils.processor.query.section.projection.keyword.TopProjectionKeyword;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class KeywordsReaderTests {
    @ParameterizedTest
    @MethodSource("okSimpleInputs")
    void okSimple(
            String input,
            List<String> variables,
            String action,
            List<ProjectionFactor> projection,
            List<Factor> byFactors,
            List<Factor> orderByFactors) {
        commonOkLogic(input, variables, action, projection, byFactors, orderByFactors);
    }

    @ParameterizedTest
    @MethodSource("okComplexInputs")
    void okComplex(
            String input,
            List<String> variables,
            String action,
            List<ProjectionFactor> projection,
            List<Factor> byFactors,
            List<Factor> orderByFactors) {
        commonOkLogic(input, variables, action, projection, byFactors, orderByFactors);
    }

    private void commonOkLogic(
            String input,
            List<String> variables,
            String action,
            List<ProjectionFactor> projections,
            List<Factor> byFactors,
            List<Factor> orderByFactors) {
        var result = new KeywordsReader(input, variables).read();
        Assertions.assertEquals(action, result.actionName());

        if (projections == null) {
            Assertions.assertNull(result.projection());
        } else {
            Assertions.assertNotNull(result.projection());
            var actualProjection = result.projection().projections();
            Assertions.assertEquals(
                    projections.size(), actualProjection.size(), () -> "For: %s\nExpected:\n%s\nActual:\n%s"
                            .formatted(input, join(projections), join(actualProjection)));

            for (int i = 0; i < projections.size(); i++) {
                Assertions.assertEquals(projections.get(i), actualProjection.get(i));
            }
        }

        if (byFactors == null) {
            Assertions.assertNull(result.bySection());
        } else {
            Assertions.assertNotNull(result.bySection());
            var actualFactors = result.bySection().factors();
            Assertions.assertEquals(byFactors.size(), actualFactors.size(), () -> "For: %s\nExpected:\n%s\nActual:\n%s"
                    .formatted(input, join(byFactors), join(actualFactors)));

            for (int i = 0; i < byFactors.size(); i++) {
                Assertions.assertEquals(byFactors.get(i), actualFactors.get(i));
            }
        }

        if (orderByFactors == null) {
            Assertions.assertNull(result.orderBySection());
        } else {
            Assertions.assertNotNull(result.orderBySection());
            var actualFactors = result.orderBySection().factors();
            Assertions.assertEquals(
                    orderByFactors.size(), actualFactors.size(), () -> "For: %s\nExpected:\n%s\nActual:\n%s"
                            .formatted(input, join(orderByFactors), join(actualFactors)));

            for (int i = 0; i < orderByFactors.size(); i++) {
                Assertions.assertEquals(orderByFactors.get(i), actualFactors.get(i));
            }
        }
    }

    static Stream<Arguments> okSimpleInputs() {
        return Stream.of(
                arguments("update", Collections.emptyList(), "update", null, null, null),
                arguments("find", Collections.emptyList(), "find", null, null, null),
                arguments(
                        "findTitle",
                        List.of("title"),
                        "find",
                        List.of(new ProjectionFactor(null, "title")),
                        null,
                        null),
                arguments(
                        "findTop3Title",
                        List.of("title"),
                        "find",
                        List.of(
                                new ProjectionFactor(new TopProjectionKeyword(3), null),
                                new ProjectionFactor(null, "title")),
                        null,
                        null),
                arguments(
                        "updateByUsername",
                        List.of("username"),
                        "update",
                        null,
                        List.of(new VariableByFactor("username", new EqualsKeyword())),
                        null),
                arguments(
                        "findByUsername",
                        List.of("username"),
                        "find",
                        null,
                        List.of(new VariableByFactor("username", new EqualsKeyword())),
                        null),
                arguments(
                        "findByUniqueId",
                        List.of("uniqueId"),
                        "find",
                        null,
                        List.of(new VariableByFactor("uniqueId", new EqualsKeyword())),
                        null),
                arguments(
                        "findByUsernameLessThan",
                        List.of("username"),
                        "find",
                        null,
                        List.of(new VariableByFactor("username", new LessThanKeyword())),
                        null),
                arguments(
                        "findByUniqueIdLessThan",
                        List.of("uniqueId"),
                        "find",
                        null,
                        List.of(new VariableByFactor("uniqueId", new LessThanKeyword())),
                        null),
                arguments(
                        "findOrderByUsername",
                        List.of("username"),
                        "find",
                        null,
                        null,
                        List.of(new VariableOrderByFactor("username", OrderDirection.DEFAULT))),
                arguments(
                        "findOrderByUsernameAsc",
                        List.of("username"),
                        "find",
                        null,
                        null,
                        List.of(new VariableOrderByFactor("username", OrderDirection.ASCENDING))),
                arguments(
                        "findByUsernameOrderByUsername",
                        List.of("username"),
                        "find",
                        null,
                        List.of(new VariableByFactor("username", new EqualsKeyword())),
                        List.of(new VariableOrderByFactor("username", OrderDirection.DEFAULT))),
                arguments(
                        "findByUsernameOrderByUsernameAsc",
                        List.of("username"),
                        "find",
                        null,
                        List.of(new VariableByFactor("username", new EqualsKeyword())),
                        List.of(new VariableOrderByFactor("username", OrderDirection.ASCENDING))),
                arguments(
                        "findByUsernameOrderByUsernameDesc",
                        List.of("username"),
                        "find",
                        null,
                        List.of(new VariableByFactor("username", new EqualsKeyword())),
                        List.of(new VariableOrderByFactor("username", OrderDirection.DESCENDING))),
                arguments(
                        "findByUniqueIdOrderByUsername",
                        List.of("uniqueId", "username"),
                        "find",
                        null,
                        List.of(new VariableByFactor("uniqueId", new EqualsKeyword())),
                        List.of(new VariableOrderByFactor("username", OrderDirection.DEFAULT))),
                arguments(
                        "findByUsernameLessThanOrderByUniqueId",
                        List.of("username", "uniqueId"),
                        "find",
                        null,
                        List.of(new VariableByFactor("username", new LessThanKeyword())),
                        List.of(new VariableOrderByFactor("uniqueId", OrderDirection.DEFAULT))),
                arguments(
                        "findByUniqueIdLessThanOrderByUniqueId",
                        List.of("uniqueId"),
                        "find",
                        null,
                        List.of(new VariableByFactor("uniqueId", new LessThanKeyword())),
                        List.of(new VariableOrderByFactor("uniqueId", OrderDirection.DEFAULT))));
    }

    static Stream<Arguments> okComplexInputs() {
        return Stream.of(
                arguments(
                        "findTop3DistinctTitle",
                        List.of("title"),
                        "find",
                        List.of(
                                new ProjectionFactor(new TopProjectionKeyword(3), null),
                                new ProjectionFactor(DistinctProjectionKeyword.INSTANCE, null),
                                new ProjectionFactor(null, "title")),
                        null,
                        null),
                arguments(
                        "findByUsernameAndPassword",
                        List.of("username", "password"),
                        "find",
                        null,
                        List.of(
                                new VariableByFactor("username", new EqualsKeyword()),
                                AndFactor.INSTANCE,
                                new VariableByFactor("password", new EqualsKeyword())),
                        null),
                arguments(
                        "findByUniqueIdAndPassword",
                        List.of("uniqueId", "password"),
                        "find",
                        null,
                        List.of(
                                new VariableByFactor("uniqueId", new EqualsKeyword()),
                                AndFactor.INSTANCE,
                                new VariableByFactor("password", new EqualsKeyword())),
                        null),
                arguments(
                        "findByUniqueIdAndUniqueName",
                        List.of("uniqueId", "uniqueName"),
                        "find",
                        null,
                        List.of(
                                new VariableByFactor("uniqueId", new EqualsKeyword()),
                                AndFactor.INSTANCE,
                                new VariableByFactor("uniqueName", new EqualsKeyword())),
                        null),
                arguments(
                        "findByUsernameLessThanAndPassword",
                        List.of("username", "password"),
                        "find",
                        null,
                        List.of(
                                new VariableByFactor("username", new LessThanKeyword()),
                                AndFactor.INSTANCE,
                                new VariableByFactor("password", new EqualsKeyword())),
                        null),
                arguments(
                        "findByUniqueIdLessThanOrMyHash",
                        List.of("uniqueId", "myHash"),
                        "find",
                        null,
                        List.of(
                                new VariableByFactor("uniqueId", new LessThanKeyword()),
                                OrFactor.INSTANCE,
                                new VariableByFactor("myHash", new EqualsKeyword())),
                        null),
                arguments(
                        "findTop3TitleByUsername",
                        List.of("title", "username"),
                        "find",
                        List.of(
                                new ProjectionFactor(new TopProjectionKeyword(3), null),
                                new ProjectionFactor(null, "title")),
                        List.of(new VariableByFactor("username", new EqualsKeyword())),
                        null),
                arguments(
                        "findTop3DistinctTitleByUsernameAndPassword",
                        List.of("title", "username", "password"),
                        "find",
                        List.of(
                                new ProjectionFactor(new TopProjectionKeyword(3), null),
                                new ProjectionFactor(DistinctProjectionKeyword.INSTANCE, null),
                                new ProjectionFactor(null, "title")),
                        List.of(
                                new VariableByFactor("username", new EqualsKeyword()),
                                AndFactor.INSTANCE,
                                new VariableByFactor("password", new EqualsKeyword())),
                        null),
                arguments(
                        "findOrderByUsernameOrEmail",
                        List.of("username", "email"),
                        "find",
                        null,
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
                        null,
                        List.of(
                                new VariableOrderByFactor("username", OrderDirection.ASCENDING),
                                OrFactor.INSTANCE,
                                new VariableOrderByFactor("password", OrderDirection.DEFAULT))),
                arguments(
                        "findByUsernameOrderByUsernameAndPassword",
                        List.of("username", "password"),
                        "find",
                        null,
                        List.of(new VariableByFactor("username", new EqualsKeyword())),
                        List.of(
                                new VariableOrderByFactor("username", OrderDirection.DEFAULT),
                                AndFactor.INSTANCE,
                                new VariableOrderByFactor("password", OrderDirection.DEFAULT))),
                arguments(
                        "findByUsernameOrderByUsernameAscOrPasswordDesc",
                        List.of("username", "password"),
                        "find",
                        null,
                        List.of(new VariableByFactor("username", new EqualsKeyword())),
                        List.of(
                                new VariableOrderByFactor("username", OrderDirection.ASCENDING),
                                OrFactor.INSTANCE,
                                new VariableOrderByFactor("password", OrderDirection.DESCENDING))),
                arguments(
                        "findByUniqueIdLessThanOrUsernameOrderByUniqueIdAndPingDesc",
                        List.of("uniqueId", "username", "ping"),
                        "find",
                        null,
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
