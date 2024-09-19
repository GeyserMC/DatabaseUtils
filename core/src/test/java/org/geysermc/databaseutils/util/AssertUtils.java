/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.AssertionFailureBuilder;

public final class AssertUtils {
    private AssertUtils() {}

    public static <T> void assertEqualsIgnoreOrder(Collection<T> expected, Collection<T> actual) {
        List<T> remaining = new ArrayList<>(expected);

        for (T element : actual) {
            if (!remaining.remove(element)) {
                AssertionFailureBuilder.assertionFailure()
                        .message("Found an element that wasn't expected")
                        .expected(expected)
                        .actual(actual)
                        .buildAndThrow();
            }
        }
        if (!remaining.isEmpty()) {
            AssertionFailureBuilder.assertionFailure()
                    .message("Expected to find " + remaining.size() + " more elements")
                    .expected(expected)
                    .actual(actual)
                    .buildAndThrow();
        }
    }
}
