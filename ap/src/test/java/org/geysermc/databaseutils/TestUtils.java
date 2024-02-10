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

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static com.google.testing.compile.JavaFileObjectSubject.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import java.util.List;
import org.geysermc.databaseutils.processor.RepositoryProcessor;
import org.geysermc.databaseutils.sql.SqlDatabase;

final class TestUtils {
    private static final List<Class<?>> DATABASE_TYPES = List.of(SqlDatabase.class);

    private TestUtils() {}

    /**
     * Tests whether the compilation is successful and that the generated impl matches the expected impl.
     */
    static void testCompilation(final String folder, final String sourceResourceSimpleName) {
        final String sourceResourceName = folder + sourceResourceSimpleName;

        final Compilation compilation = javac().withProcessors(new RepositoryProcessor())
                .compile(JavaFileObjects.forResource(sourceResourceName + ".java"));

        assertThat(compilation).succeeded();

        // every db type has a db class and a repository class
        assertEquals(
                DATABASE_TYPES.size() * 2, compilation.generatedSourceFiles().size(), "Generated source file count");

        for (Class<?> databaseTypeClass : DATABASE_TYPES) {
            var databaseType = databaseTypeClass.getSimpleName().replace("Database", "");
            // repo
            var repoImplName = sourceResourceName + databaseType + "Impl";
            var generatedRepo = compilation.generatedSourceFile(repoImplName);
            assertTrue(generatedRepo.isPresent(), "Expected " + repoImplName + " to be generated");
            assertThat(generatedRepo.get()).hasSourceEquivalentTo(JavaFileObjects.forResource(repoImplName + ".java"));

            // db
            var expectedDatabaseImpl = folder + databaseTypeClass.getSimpleName() + "Generated";
            var actualDatabaseImpl = compilation.generatedSourceFile(
                    databaseTypeClass.getCanonicalName().replace('.', '/') + "Generated");
            assertTrue(actualDatabaseImpl.isPresent(), "Expected " + expectedDatabaseImpl + " to be generated");
            assertThat(actualDatabaseImpl.get())
                    .hasSourceEquivalentTo(JavaFileObjects.forResource(expectedDatabaseImpl + ".java"));
        }
    }
}
