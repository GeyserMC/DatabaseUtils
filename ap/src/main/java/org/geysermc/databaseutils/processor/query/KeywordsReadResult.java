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

import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.databaseutils.processor.query.section.BySection;
import org.geysermc.databaseutils.processor.query.section.OrderBySection;
import org.geysermc.databaseutils.processor.query.section.ProjectionSection;

public record KeywordsReadResult(
        String actionName,
        @Nullable ProjectionSection projection,
        @Nullable BySection bySection,
        @Nullable OrderBySection orderBySection) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ProjectionSection projection;
        private BySection bySection;
        private OrderBySection orderBySection;

        private Builder() {}

        public ProjectionSection projection() {
            return projection;
        }

        public Builder projection(ProjectionSection projection) {
            if (this.projection != null) {
                throw new IllegalStateException("Cannot redefine projection!");
            }
            this.projection = projection;
            return this;
        }

        public BySection bySection() {
            return bySection;
        }

        public Builder bySection(BySection bySection) {
            if (this.bySection != null) {
                throw new IllegalStateException("Cannot redefine by section!");
            }
            this.bySection = bySection;
            return this;
        }

        public OrderBySection orderBySection() {
            return orderBySection;
        }

        public Builder orderBySection(OrderBySection orderBySection) {
            if (this.orderBySection != null) {
                throw new IllegalStateException("Cannot redefine orderBy section!");
            }
            this.orderBySection = orderBySection;
            return this;
        }

        public KeywordsReadResult build(String actionName) {
            return new KeywordsReadResult(actionName, projection, bySection, orderBySection);
        }
    }
}
