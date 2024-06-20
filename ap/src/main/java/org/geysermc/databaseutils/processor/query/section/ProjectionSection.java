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
package org.geysermc.databaseutils.processor.query.section;

import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.databaseutils.processor.query.section.factor.Factor;
import org.geysermc.databaseutils.processor.query.section.factor.ProjectionFactor;
import org.geysermc.databaseutils.processor.query.section.projection.ProjectionKeyword;
import org.geysermc.databaseutils.processor.query.section.projection.keyword.DistinctProjectionKeyword;

public record ProjectionSection(List<@NonNull ProjectionFactor> projections) {
    public ProjectionSection(@NonNull List<@NonNull ProjectionFactor> projections) {
        this.projections = projections;
    }

    public static ProjectionSection from(@NonNull List<@NonNull Factor> factors) {
        if (factors.isEmpty()) {
            return null;
        }
        return new ProjectionSection(
                factors.stream().map(factor -> (ProjectionFactor) factor).toList());
    }

    public String columnName() {
        return projections.stream()
                .<String>mapMulti((factor, results) -> {
                    if (factor.keyword() == null) {
                        results.accept(factor.columnName());
                    }
                })
                .findFirst()
                .orElse(null);
    }

    public DistinctProjectionKeyword distinct() {
        return projections.stream()
                .<DistinctProjectionKeyword>mapMulti((factor, results) -> {
                    if (factor.keyword() instanceof DistinctProjectionKeyword keyword) {
                        results.accept(keyword);
                    }
                })
                .findFirst()
                .orElse(null);
    }

    public @NonNull List<@NonNull ProjectionKeyword> notDistinctProjectionKeywords() {
        return projections.stream()
                .<ProjectionKeyword>mapMulti((factor, results) -> {
                    if (factor.columnName() != null && !(factor.keyword() instanceof DistinctProjectionKeyword)) {
                        results.accept(factor.keyword());
                    }
                })
                .toList();
    }
}
