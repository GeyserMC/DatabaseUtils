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
package org.geysermc.databaseutils;

import java.util.List;
import java.util.function.Supplier;
import org.geysermc.databaseutils.sql.SqlDatabase;
import org.geysermc.databaseutils.sql.SqlDatabasePresent;

final class DatabaseRegistry {
    private static final List<Supplier<Database>> TYPES = List.of(SqlDatabase::new);
    private static final List<DatabaseTypePresent> TYPE_PRESENT = List.of(new SqlDatabasePresent());

    public static Database firstPresentDatabase() {
        for (int i = 0; i < TYPE_PRESENT.size(); i++) {
            if (TYPE_PRESENT.get(i).isPresent()) {
                return TYPES.get(i).get();
            }
        }
        return null;
    }
}
