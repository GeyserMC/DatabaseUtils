/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class StringUtils {
    private StringUtils() {}

    public static String capitalize(CharSequence input) {
        return String.valueOf(Character.toUpperCase(input.charAt(0))) + input.subSequence(1, input.length());
    }

    public static String uncapitalize(CharSequence input) {
        return String.valueOf(Character.toLowerCase(input.charAt(0))) + input.subSequence(1, input.length());
    }

    public static String screamingSnakeCaseToPascalCase(String input) {
        String[] parts = input.toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            result.append(capitalize(part));
        }
        return result.toString();
    }

    public static List<String> repeat(String string, int count) {
        var list = new ArrayList<String>(count);
        for (int i = 0; i < count; i++) {
            list.add(string);
        }
        return list;
    }

    public static <T> LargestMatchResult<T> largestMatch(List<String> parts, int offset, Function<String, T> matcher) {
        var greatestMatch = offset;
        T response = null;
        var workingVariable = new StringBuilder();

        for (int i = offset; i < parts.size(); i++) {
            var current = parts.get(i);
            workingVariable.append(current);

            var finalVariable = workingVariable.toString();
            var finalResult = matcher.apply(finalVariable);
            if (finalResult != null) {
                greatestMatch = i;
                response = finalResult;
            }
        }

        if (response == null) {
            return null;
        }
        return new LargestMatchResult<>(response, greatestMatch);
    }

    public record LargestMatchResult<T>(T match, int offset) {}
}
