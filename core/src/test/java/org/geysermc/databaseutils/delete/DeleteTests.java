/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.delete;

import static org.geysermc.databaseutils.util.AssertUtils.assertEqualsIgnoreOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
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
    Stream<DynamicTest> deleteSingleEntityJustCheckKeys() {
        // current behaviour is that value of the keys determine whether an item is deleted.
        // not the match of every data type
        return context.allTypesFor(DeleteRepository.class, repository -> {
            repository.insert(new TestEntity(0, "hello", "world!", null));
            repository.insert(new TestEntity(1, "hello", "world!", null));
            repository.insert(new TestEntity(2, "hello", "world!", null));
            assertTrue(repository.existsByAAndB(0, "hello"));
            assertTrue(repository.existsByAAndB(1, "hello"));
            assertTrue(repository.existsByAAndB(2, "hello"));

            repository.delete(new TestEntity(1, "hello", "steve!", UUID.nameUUIDFromBytes("steve".getBytes())));
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
                    repository.insert(new TestEntity(1, "hello", "world!", null));
                    repository.insert(new TestEntity(2, "hello", "world!", null));
                    repository.insert(new TestEntity(0, "hello", "world!", null));

                    assertEquals(1, repository.deleteFirstByB("hello"));

                    // depending on the dialect either the first inserted item is deleted or
                    // the one with the lowest index
                    if (repository.existsByAAndB(1, "hello")) {
                        assertFalse(repository.existsByAAndB(0, "hello"));
                    } else {
                        assertTrue(repository.existsByAAndB(0, "hello"));
                    }
                    assertTrue(repository.existsByAAndB(2, "hello"));
                },
                DatabaseType.ORACLE_DATABASE,
                DatabaseType.POSTGRESQL,
                DatabaseType.SQLITE,
                DatabaseType.SQL_SERVER);
        // delete limit is a flag that needs to be enabled during compiling on sqlite, and on Oracle and Postgres it
        // doesn't exist. So we have to work around this in the future by doing a subquery
    }

    @TestFactory
    Stream<DynamicTest> deleteFirstOrderBy() {
        // todo indexes are not yet added for sql types
        return context.allTypesForOnly(
                DeleteRepository.class,
                repository -> {
                    repository.insert(new TestEntity(1, "hello", "world!", null));
                    repository.insert(new TestEntity(2, "hello", "world!", null));
                    repository.insert(new TestEntity(0, "hello", "world!", null));

                    assertEquals(1, repository.deleteFirstByBOrderByA("hello"));
                    assertFalse(repository.existsByAAndB(0, "hello"));
                    assertTrue(repository.existsByAAndB(1, "hello"));
                    assertTrue(repository.existsByAAndB(2, "hello"));
                },
                DatabaseType.MONGODB);
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
                DatabaseType.MYSQL);
        // see readme why those are excluded for now
    }

    @TestFactory
    Stream<DynamicTest> deleteReturningList() {
        return context.allTypesForBut(
                DeleteRepository.class,
                repository -> {
                    repository.insert(new TestEntity(1, "hello", "steve!", null));
                    repository.insert(new TestEntity(2, "hi", "world!", null));
                    repository.insert(new TestEntity(0, "hello", "world!", null));

                    var returning = repository.deleteReturningList("hello");

                    // all dialects except OracleDB returns it in insertion order,
                    // so since we support OracleDB we can't make order assumptions.
                    assertEqualsIgnoreOrder(
                            List.of(
                                    new TestEntity(1, "hello", "steve!", null),
                                    new TestEntity(0, "hello", "world!", null)),
                            returning);

                    assertTrue(repository.existsByAAndB(2, "hi"));
                    assertFalse(repository.existsByAAndB(0, "hello"));
                    assertFalse(repository.existsByAAndB(1, "hello"));
                },
                DatabaseType.H2,
                DatabaseType.MYSQL);
        // see readme why those are excluded for now
    }

    @TestFactory
    Stream<DynamicTest> deleteFirstWithOrderReturning() {
        // todo allow dialect specific impls, then impl this
        return context.allTypesForBut(
                DeleteRepository.class,
                repository -> {
                    repository.insert(new TestEntity(1, "hello", "world!", null));
                    repository.insert(new TestEntity(2, "hello", "world!", null));
                    repository.insert(new TestEntity(0, "hello", "world!", null));

                    assertEquals(
                            new TestEntity(0, "hello", "world!", null),
                            repository.deleteFirstWithOrderReturning("hello"));
                    assertFalse(repository.existsByAAndB(0, "hello"));
                    assertTrue(repository.existsByAAndB(1, "hello"));
                    assertTrue(repository.existsByAAndB(2, "hello"));
                },
                DatabaseType.ORACLE_DATABASE,
                DatabaseType.POSTGRESQL,
                DatabaseType.SQLITE,
                DatabaseType.H2,
                DatabaseType.MYSQL,
                DatabaseType.MONGODB,
                DatabaseType.MARIADB,
                DatabaseType.SQL_SERVER);
    }
}
