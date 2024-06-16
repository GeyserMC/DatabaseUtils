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
package org.geysermc.databaseutils.processor.info;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record EntityInfo(
        String name,
        CharSequence className,
        List<ColumnInfo> columns,
        List<IndexInfo> indexes,
        List<CharSequence> keys) {
    public ColumnInfo columnFor(CharSequence columnName) {
        for (ColumnInfo column : columns) {
            if (column.name().contentEquals(columnName)) {
                return column;
            }
        }
        return null;
    }

    public List<ColumnInfo> keyColumns() {
        return keys.stream().map(this::columnFor).collect(Collectors.toList());
    }

    public List<ColumnInfo> notKeyColumns() {
        return columns().stream()
                .filter(column -> !keys.contains(column.name()))
                .toList();
    }

    public List<ColumnInfo> notKeyFirstColumns() {
        var combined = new ArrayList<ColumnInfo>();
        combined.addAll(notKeyColumns());
        combined.addAll(keyColumns());
        return combined;
    }
}
