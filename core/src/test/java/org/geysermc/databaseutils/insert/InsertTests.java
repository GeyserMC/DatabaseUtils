/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.insert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;
import org.geysermc.databaseutils.TestContext;
import org.geysermc.databaseutils.entity.TestEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

final class InsertTests {
    static TestContext context = TestContext.INSTANCE;

    @BeforeAll
    static void setUp() {
        context.start(InsertRepository.class);
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
    Stream<DynamicTest> insert() {
        return context.allTypesFor(InsertRepository.class, repository -> {
            assertFalse(repository.existsByAAndB(0, "hello"));
            assertFalse(repository.existsByAAndB(1, "hello"));
            assertFalse(repository.existsByAAndB(2, "hello"));

            repository.insert(new TestEntity(0, "hello", "world!", null));

            assertTrue(repository.existsByAAndB(0, "hello"));
            assertFalse(repository.existsByAAndB(1, "hello"));
            assertFalse(repository.existsByAAndB(2, "hello"));

            repository.insert(new TestEntity(1, "hello", "world!", null));
            repository.insert(new TestEntity(2, "hello", "world!", null));

            assertTrue(repository.existsByAAndB(0, "hello"));
            assertTrue(repository.existsByAAndB(1, "hello"));
            assertTrue(repository.existsByAAndB(2, "hello"));
        });
    }

    @TestFactory
    Stream<DynamicTest> insertMany() {
        return context.allTypesFor(InsertRepository.class, repository -> {
            assertFalse(repository.existsByAAndB(0, "hello"));
            assertFalse(repository.existsByAAndB(1, "hello"));
            assertFalse(repository.existsByAAndB(2, "hello"));

            repository.insert(
                    List.of(new TestEntity(0, "hello", "world!", null), new TestEntity(1, "hello", "world!", null)));

            assertTrue(repository.existsByAAndB(0, "hello"));
            assertTrue(repository.existsByAAndB(1, "hello"));
            assertFalse(repository.existsByAAndB(2, "hello"));

            repository.insert(List.of(new TestEntity(2, "hello", "world!", null)));

            assertTrue(repository.existsByAAndB(0, "hello"));
            assertTrue(repository.existsByAAndB(1, "hello"));
            assertTrue(repository.existsByAAndB(2, "hello"));

            repository.insert(List.of());

            assertTrue(repository.existsByAAndB(0, "hello"));
            assertTrue(repository.existsByAAndB(1, "hello"));
            assertTrue(repository.existsByAAndB(2, "hello"));
        });
    }

    @TestFactory
    Stream<DynamicTest> insertWithCount() {
        return context.allTypesFor(InsertRepository.class, repository -> {
            assertFalse(repository.existsByAAndB(0, "hello"));
            assertFalse(repository.existsByAAndB(1, "hello"));
            assertFalse(repository.existsByAAndB(2, "hello"));

            assertEquals(1, repository.insertWithCount(new TestEntity(0, "hello", "world!", null)));

            assertTrue(repository.existsByAAndB(0, "hello"));
            assertFalse(repository.existsByAAndB(1, "hello"));
            assertFalse(repository.existsByAAndB(2, "hello"));

            assertEquals(1, repository.insertWithCount(new TestEntity(1, "hello", "world!", null)));
            assertEquals(1, repository.insertWithCount(new TestEntity(2, "hello", "world!", null)));

            assertTrue(repository.existsByAAndB(0, "hello"));
            assertTrue(repository.existsByAAndB(1, "hello"));
            assertTrue(repository.existsByAAndB(2, "hello"));
        });
    }

    @TestFactory
    Stream<DynamicTest> insertWithCountMany() {
        return context.allTypesFor(InsertRepository.class, repository -> {
            assertFalse(repository.existsByAAndB(0, "hello"));
            assertFalse(repository.existsByAAndB(1, "hello"));
            assertFalse(repository.existsByAAndB(2, "hello"));

            assertEquals(
                    2,
                    repository.insertWithCount(List.of(
                            new TestEntity(0, "hello", "world!", null), new TestEntity(1, "hello", "world!", null))));

            assertTrue(repository.existsByAAndB(0, "hello"));
            assertTrue(repository.existsByAAndB(1, "hello"));
            assertFalse(repository.existsByAAndB(2, "hello"));

            assertEquals(1, repository.insertWithCount(new TestEntity(2, "hello", "world!", null)));

            assertTrue(repository.existsByAAndB(0, "hello"));
            assertTrue(repository.existsByAAndB(1, "hello"));
            assertTrue(repository.existsByAAndB(2, "hello"));

            assertEquals(0, repository.insertWithCount(List.of()));

            assertTrue(repository.existsByAAndB(0, "hello"));
            assertTrue(repository.existsByAAndB(1, "hello"));
            assertTrue(repository.existsByAAndB(2, "hello"));
        });
    }
}
