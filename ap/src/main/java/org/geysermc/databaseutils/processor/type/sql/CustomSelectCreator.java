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
package org.geysermc.databaseutils.processor.type.sql;

import ca.krasnay.sqlbuilder.Predicate;
import ca.krasnay.sqlbuilder.SelectCreator;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

public final class CustomSelectCreator extends SelectCreator {
    @Serial
    private static final long serialVersionUID = 8241972814224385840L;

    @Override
    public CustomSelectCreator column(String name) {
        return (CustomSelectCreator) super.column(name);
    }

    @Override
    public CustomSelectCreator from(String table) {
        return (CustomSelectCreator) super.from(table);
    }

    @Override
    public CustomSelectCreator where(Predicate predicate) {
        return (CustomSelectCreator) super.where(predicate);
    }

    public SpecResult toSpecResult() {
        var parameters = new ArrayList<String>();
        var sql = getBuilder().toString();
        for (var entry : getPreparedStatementCreator().getParameterMap().entrySet()) {
            sql = sql.replace(':' + entry.getKey(), "?");
            parameters.add(entry.getValue().toString());
        }
        return new SpecResult("\"" + sql + "\"", parameters);
    }

    public record SpecResult(String query, List<String> parameterNames) {}
}
