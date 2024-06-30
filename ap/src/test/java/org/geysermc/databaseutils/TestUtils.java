/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
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
import java.util.Map;
import org.geysermc.databaseutils.mongo.MongodbDatabase;
import org.geysermc.databaseutils.processor.RepositoryProcessor;
import org.geysermc.databaseutils.sql.SqlDatabase;

final class TestUtils {
    private static final Map<String, Class<?>> DATABASE_TYPES =
            Map.of("Sql", SqlDatabase.class, "Mongo", MongodbDatabase.class);

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

        for (var entry : DATABASE_TYPES.entrySet()) {
            var databaseType = entry.getKey() + "Database";
            // repo
            var repoImplName = sourceResourceName + entry.getKey() + "Impl";
            var generatedRepo = compilation.generatedSourceFile(repoImplName);
            assertTrue(generatedRepo.isPresent(), "Expected " + repoImplName + " to be generated");
            assertThat(generatedRepo.get()).hasSourceEquivalentTo(JavaFileObjects.forResource(repoImplName + ".java"));

            // db
            var expectedDatabaseImpl = folder + databaseType + "Generated";
            var actualDatabaseImpl = compilation.generatedSourceFile(
                    entry.getValue().getPackageName().replace('.', '/') + "/" + databaseType + "Generated");
            assertTrue(actualDatabaseImpl.isPresent(), "Expected " + expectedDatabaseImpl + " to be generated");
            assertThat(actualDatabaseImpl.get())
                    .hasSourceEquivalentTo(JavaFileObjects.forResource(expectedDatabaseImpl + ".java"));
        }
    }
}
