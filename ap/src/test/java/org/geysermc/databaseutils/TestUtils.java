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

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import org.geysermc.databaseutils.processor.RepositoryProcessor;

final class TestUtils {
    private static final String PACKAGE = "org.geysermc.databaseutils.generated.";

    private TestUtils() {}

    /**
     * Tests whether the compilation is successful and that the generated impl matches the expected impl.
     */
    static Compilation testCompilation(final String sourceResourceName) {
        final Compilation compilation = javac().withProcessors(new RepositoryProcessor())
                .compile(JavaFileObjects.forResource(sourceResourceName + ".java"));

        final String targetResourceName = sourceResourceName + "Impl";

        final String targetSourceSimpleName = targetResourceName.substring(targetResourceName.lastIndexOf("/") + 1);
        final String targetSourceName = PACKAGE + targetSourceSimpleName;

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile(targetSourceName)
                .hasSourceEquivalentTo(JavaFileObjects.forResource(targetResourceName + ".java"));

        return compilation;
    }
}
