/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.delete;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.geysermc.databaseutils.DatabaseType;
import org.geysermc.databaseutils.TestContext;
import org.geysermc.databaseutils.entity.TestEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

final class DeleteTests {
    static TestContext context = TestContext.INSTANCE;

    @BeforeAll
    static void setUp() {
        context.start(DeleteRepository.class);
    }

    @AfterAll
    static void tearDown() {
        context.stop();
    }

    @AfterEach
    void clearUp() {
        context.deleteRows();
    }

    @TestFactory
    Stream<DynamicTest> delete() {
        return context.allTypesFor(DeleteRepository.class, repository -> {
            repository.insert(new TestEntity(0, "hello", "world!", null));
            repository.insert(new TestEntity(1, "hello", "world!", null));
            repository.insert(new TestEntity(2, "hello", "world!", null));

            assertTrue(repository.existsByAAndB(0, "hello"));
            assertTrue(repository.existsByAAndB(1, "hello"));
            assertTrue(repository.existsByAAndB(2, "hello"));
            repository.delete();
            assertFalse(repository.existsByAAndB(0, "hello"));
            assertFalse(repository.existsByAAndB(1, "hello"));
            assertFalse(repository.existsByAAndB(2, "hello"));
        });
    }

    @TestFactory
    Stream<DynamicTest> deleteSingleEntity() {
        return context.allTypesFor(DeleteRepository.class, repository -> {
            repository.insert(new TestEntity(0, "hello", "world!", null));
            repository.insert(new TestEntity(1, "hello", "world!", null));
            repository.insert(new TestEntity(2, "hello", "world!", null));

            assertTrue(repository.existsByAAndB(1, "hello"));
            repository.delete(new TestEntity(1, "hello", "world!", null));
            assertFalse(repository.existsByAAndB(1, "hello"));
            assertTrue(repository.existsByAAndB(0, "hello"));
            assertTrue(repository.existsByAAndB(2, "hello"));
        });
    }

    @TestFactory
    Stream<DynamicTest> deleteSingleByKey() {
        return context.allTypesFor(DeleteRepository.class, repository -> {
            repository.insert(new TestEntity(0, "hello", "world!", null));
            repository.insert(new TestEntity(1, "hello", "world!", null));
            repository.insert(new TestEntity(2, "hello", "world!", null));

            assertTrue(repository.existsByAAndB(1, "hello"));
            repository.deleteByAAndB(1, "hello");
            assertFalse(repository.existsByAAndB(1, "hello"));
            assertTrue(repository.existsByAAndB(0, "hello"));
            assertTrue(repository.existsByAAndB(2, "hello"));
        });
    }

    @TestFactory
    Stream<DynamicTest> deleteNonUnique() {
        return context.allTypesFor(DeleteRepository.class, repository -> {
            repository.insert(new TestEntity(0, "hello", "world!", null));
            repository.insert(new TestEntity(1, "hello", "world!", null));
            repository.insert(new TestEntity(2, "hello", "world!", null));

            assertTrue(repository.existsByAAndB(0, "hello"));
            assertTrue(repository.existsByAAndB(1, "hello"));
            assertTrue(repository.existsByAAndB(2, "hello"));
            assertEquals(3, repository.deleteByB("hello"));
            assertFalse(repository.existsByAAndB(0, "hello"));
            assertFalse(repository.existsByAAndB(1, "hello"));
            assertFalse(repository.existsByAAndB(2, "hello"));
        });
    }

    @TestFactory
    Stream<DynamicTest> deleteFirst() {
        return context.allTypesForBut(
                DeleteRepository.class,
                repository -> {
                    repository.insert(new TestEntity(0, "hello", "world!", null));
                    repository.insert(new TestEntity(1, "hello", "world!", null));
                    repository.insert(new TestEntity(2, "hello", "world!", null));

                    assertTrue(repository.existsByAAndB(1, "hello"));
                    assertEquals(1, repository.deleteFirstByB("hello"));
                    assertFalse(repository.existsByAAndB(0, "hello"));
                    assertTrue(repository.existsByAAndB(1, "hello"));
                    assertTrue(repository.existsByAAndB(2, "hello"));
                },
                DatabaseType.ORACLE_DATABASE,
                DatabaseType.POSTGRESQL,
                DatabaseType.SQLITE);
        // delete limit is a flag that needs to be enabled during compiling on sqlite, and on Oracle and Postgres it
        // doesn't exist. So we have to work around this in the future by doing a subquery
    }

    @TestFactory
    Stream<DynamicTest> deleteReturning() {
        return context.allTypesForBut(
                DeleteRepository.class,
                repository -> {
                    repository.insert(new TestEntity(0, "hello", "world!", null));
                    repository.insert(new TestEntity(1, "hello", "world!", null));
                    repository.insert(new TestEntity(2, "hello", "world!", null));

                    assertTrue(repository.existsByAAndB(1, "hello"));
                    var returning = repository.deleteReturning(1, "hello");
                    assertEquals(new TestEntity(1, "hello", "world!", null), returning);

                    assertFalse(repository.existsByAAndB(1, "hello"));
                    assertTrue(repository.existsByAAndB(0, "hello"));
                    assertTrue(repository.existsByAAndB(2, "hello"));
                },
                DatabaseType.H2,
                DatabaseType.MYSQL,
                DatabaseType.ORACLE_DATABASE);
        // see readme why those are excluded for now
    }
}
