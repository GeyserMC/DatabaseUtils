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

import java.util.List;
import java.util.stream.Stream;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.geysermc.databaseutils.processor.info.ColumnInfo;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.query.section.factor.Factor;
import org.geysermc.databaseutils.processor.query.section.factor.VariableByFactor;

public record QueryInfo(EntityInfo entityInfo, KeywordsReadResult result, ExecutableElement element) {
    public String tableName() {
        return entityInfo.name();
    }

    public CharSequence entityType() {
        return entityInfo.className();
    }

    public List<ColumnInfo> columns() {
        return entityInfo.columns();
    }

    public ColumnInfo columnFor(CharSequence columnName) {
        for (ColumnInfo column : columns()) {
            if (column.name().contentEquals(columnName)) {
                return column;
            }
        }
        return null;
    }

    public boolean hasBySection() {
        return result.bySection() != null;
    }

    public @MonotonicNonNull List<Factor> bySectionFactors() {
        return result.bySection() != null ? result.bySection().factors() : null;
    }

    public void requireBySection() {
        if (bySectionFactors() == null || bySectionFactors().isEmpty()) {
            throw new IllegalStateException("This query requires a By section");
        }
    }

    public List<VariableByFactor> byVariables() {
        requireBySection();

        return bySectionFactors().stream()
                .flatMap(section -> {
                    if (section instanceof VariableByFactor variable) {
                        return Stream.of(variable);
                    }
                    return null;
                })
                .toList();
    }

    public CharSequence parameterName(int index) {
        return element.getParameters().get(index).getSimpleName();
    }
}
