/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
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
                .useDefaultCredentials(true)
                .type(DatabaseWithDialectType.MONGODB)
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
