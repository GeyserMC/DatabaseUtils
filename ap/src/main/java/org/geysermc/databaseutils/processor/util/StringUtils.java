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
package org.geysermc.databaseutils.processor.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class StringUtils {
    private StringUtils() {}

    public static String capitalize(CharSequence input) {
        return String.valueOf(Character.toUpperCase(input.charAt(0))) + input.subSequence(1, input.length());
    }

    public static String uncapitalize(CharSequence input) {
        return String.valueOf(Character.toLowerCase(input.charAt(0))) + input.subSequence(1, input.length());
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
