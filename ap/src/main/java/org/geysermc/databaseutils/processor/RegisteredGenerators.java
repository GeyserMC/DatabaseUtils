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
package org.geysermc.databaseutils.processor;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.geysermc.databaseutils.processor.type.DatabaseGenerator;
import org.geysermc.databaseutils.processor.type.RepositoryGenerator;
import org.geysermc.databaseutils.processor.type.SqlDatabaseGenerator;
import org.geysermc.databaseutils.processor.type.SqlRepositoryGenerator;

final class RegisteredGenerators {
    // both the database and the repository generators have to be on the same indexes

    private static final List<Supplier<DatabaseGenerator>> DATABASE_GENERATORS = List.of(SqlDatabaseGenerator::new);
    private static final List<Supplier<RepositoryGenerator>> REPOSITORY_GENERATORS =
            List.of(SqlRepositoryGenerator::new);

    private RegisteredGenerators() {}

    public static List<DatabaseGenerator> databaseGenerators() {
        return DATABASE_GENERATORS.stream().map(Supplier::get).collect(Collectors.toList());
    }

    public static List<RepositoryGenerator> repositoryGenerators() {
        return REPOSITORY_GENERATORS.stream().map(Supplier::get).collect(Collectors.toList());
    }

    public static int generatorCount() {
        return DATABASE_GENERATORS.size();
    }
}
