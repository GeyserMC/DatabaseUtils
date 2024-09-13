/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.exists;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.geysermc.databaseutils.TestContext;
import org.geysermc.databaseutils.entity.TestEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

final class ExistsTests {
    static TestContext context = TestContext.INSTANCE;

    @BeforeAll
    static void setUp() {
        context.start(ExistsRepository.class);
    }

    @AfterAll
    static void tearDown() {
        context.stop();
    }

    @AfterEach
    void cleanUp() {
        context.deleteRows();
    }

    @TestFactory
    Stream<DynamicTest> existsSingleEntity() {
        return context.allTypesFor(ExistsRepository.class, repository -> {
            assertFalse(repository.exists(new TestEntity(0, "hello", "world!", null)));
            assertFalse(repository.exists(new TestEntity(1, "hello", "world!", null)));
            assertFalse(repository.exists(new TestEntity(2, "hello", "world!", null)));

            repository.insert(new TestEntity(0, "hello", "world!", null));
            assertTrue(repository.exists(new TestEntity(0, "hello", "world!", null)));
            assertFalse(repository.exists(new TestEntity(1, "hello", "world!", null)));
            assertFalse(repository.exists(new TestEntity(2, "hello", "world!", null)));

            repository.insert(new TestEntity(1, "hello", "world!", null));
            repository.insert(new TestEntity(2, "hello", "world!", null));
            assertTrue(repository.exists(new TestEntity(0, "hello", "world!", null)));
            assertTrue(repository.exists(new TestEntity(1, "hello", "world!", null)));
            assertTrue(repository.exists(new TestEntity(2, "hello", "world!", null)));
        });
    }

    @TestFactory
    Stream<DynamicTest> existsSingleEntityJustCheckKeys() {
        return context.allTypesFor(ExistsRepository.class, repository -> {
            assertFalse(repository.exists(new TestEntity(0, "hello", "world!", null)));
            assertFalse(repository.exists(new TestEntity(1, "hello", "world!", null)));
            assertFalse(repository.exists(new TestEntity(2, "hello", "world!", null)));

            repository.insert(new TestEntity(0, "hello", "world!", null));
            assertTrue(repository.exists(new TestEntity(0, "hello", "steve!", null)));
            assertFalse(repository.exists(new TestEntity(1, "hello", "alex!", null)));
            assertFalse(repository.exists(new TestEntity(2, "hello", "everyone!", null)));

            repository.insert(new TestEntity(1, "hello", "world!", null));
            repository.insert(new TestEntity(2, "hello", "world!", null));
            assertTrue(repository.exists(new TestEntity(0, "hello", "alex!", null)));
            assertTrue(repository.exists(new TestEntity(1, "hello", "everyone!", null)));
            assertTrue(repository.exists(new TestEntity(2, "hello", "steve!", null)));
        });
    }
}
