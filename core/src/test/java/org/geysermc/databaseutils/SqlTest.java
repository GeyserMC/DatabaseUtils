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

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.geysermc.databaseutils.data.BasicRepository;
import org.geysermc.databaseutils.data.TestEntity;
import org.junit.jupiter.api.Test;

final class SqlTest {
    @Test
    void hello() throws ExecutionException, InterruptedException {
        var utils = DatabaseUtils.builder()
                .config(new DatabaseConfig("jdbc:h2:./test", "sa", "", "hello", 2))
                .executorService(Executors.newCachedThreadPool())
                .build();
        utils.start();
        var repo = utils.repositoryFor(BasicRepository.class);

        var created = new TestEntity(3, "", "what's up?", UUID.randomUUID());
        System.out.println(created);

        repo.insert(created).get();

        var result = repo.findByAAndB(3, "").get();
        System.out.println(result);

        repo.update(new TestEntity(result.a(), result.b(), result.c() + "h", result.d()))
                .get();
        var original = result;

        result = repo.findByAAndB(3, "").get();
        System.out.println(result);

        repo.delete(result).get();

        result = repo.findByAAndB(3, "").get();
        System.out.println(result);

        repo.insert(original).get();

        result = repo.findByAAndB(3, "").get();
        System.out.println(result);

        repo.delete(original).get();
    }
}
