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
package org.geysermc.databaseutils.processor.query.section.projection;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.databaseutils.processor.query.section.projection.keyword.AvgProjectionKeyword;
import org.geysermc.databaseutils.processor.query.section.projection.keyword.DistinctProjectionKeyword;
import org.geysermc.databaseutils.processor.query.section.projection.keyword.TopProjectionKeyword;

public class ProjectionKeywordRegistry {
    private static final Map<Pattern, Supplier<ProjectionKeyword>> REGISTRY = new HashMap<>();

    public static @Nullable ProjectionKeyword findByName(String keyword) {
        for (var entry : REGISTRY.entrySet()) {
            if (entry.getKey().matcher(keyword).matches()) {
                var instance = entry.getValue().get();
                instance.setValue(keyword);
                return instance;
            }
        }
        return null;
    }

    private static void register(Supplier<ProjectionKeyword> keywordSupplier) {
        var instance = keywordSupplier.get();
        REGISTRY.put(Pattern.compile(instance.name()), keywordSupplier);
    }

    static {
        register(DistinctProjectionKeyword::new);
        register(AvgProjectionKeyword::new);
        register(TopProjectionKeyword::new);
    }
}
