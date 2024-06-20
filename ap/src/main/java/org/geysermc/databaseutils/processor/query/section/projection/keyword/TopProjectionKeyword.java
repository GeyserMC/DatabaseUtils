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
package org.geysermc.databaseutils.processor.query.section.projection.keyword;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.databaseutils.processor.query.section.projection.ProjectionKeyword;
import org.geysermc.databaseutils.processor.query.section.projection.ProjectionKeywordCategory;

public class TopProjectionKeyword extends ProjectionKeyword {
    private int limit;

    public TopProjectionKeyword() {
        super("Top[1-9][0-9]*", ProjectionKeywordCategory.LIMIT);
    }

    public TopProjectionKeyword(int limit) {
        this();
        limit(limit);
    }

    public int limit() {
        return limit;
    }

    public void limit(int limit) {
        this.limit = limit;
    }

    public void setValue(@NonNull String fullKeyword) {
        limit(Integer.parseInt(fullKeyword.substring(3)));
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }
        return ((TopProjectionKeyword) o).limit == limit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name(), limit);
    }
}
