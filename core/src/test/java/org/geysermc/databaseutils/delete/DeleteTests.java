/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.delete;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.stream.Stream;
import org.geysermc.databaseutils.DatabaseType;
import org.geysermc.databaseutils.TestContext;
import org.geysermc.databaseutils.delete.repository.DeleteRepository;
import org.geysermc.databaseutils.entity.TestEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

final class DeleteTests {
    static TestContext context = new TestContext();
    // static TestContext context = new TestContext(DatabaseType.SQLITE, DatabaseType.H2, DatabaseType.MONGODB);

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

            assertNotNull(repository.findByAAndB(0, "hello"));
            assertNotNull(repository.findByAAndB(1, "hello"));
            assertNotNull(repository.findByAAndB(2, "hello"));
            repository.delete();
            assertNull(repository.findByAAndB(0, "hello"));
            assertNull(repository.findByAAndB(1, "hello"));
            assertNull(repository.findByAAndB(2, "hello"));
        });
    }

    @TestFactory
    Stream<DynamicTest> deleteSingleEntity() {
        return context.allTypesFor(DeleteRepository.class, repository -> {
            repository.insert(new TestEntity(0, "hello", "world!", null));
            repository.insert(new TestEntity(1, "hello", "world!", null));
            repository.insert(new TestEntity(2, "hello", "world!", null));

            assertNotNull(repository.findByAAndB(1, "hello"));
            repository.delete(new TestEntity(1, "hello", "world!", null));
            assertNull(repository.findByAAndB(1, "hello"));
            assertNotNull(repository.findByAAndB(0, "hello"));
            assertNotNull(repository.findByAAndB(2, "hello"));
        });
    }

    @TestFactory
    Stream<DynamicTest> deleteSingleByKey() {
        return context.allTypesFor(DeleteRepository.class, repository -> {
            repository.insert(new TestEntity(0, "hello", "world!", null));
            repository.insert(new TestEntity(1, "hello", "world!", null));
            repository.insert(new TestEntity(2, "hello", "world!", null));

            assertNotNull(repository.findByAAndB(1, "hello"));
            repository.deleteByAAndB(1, "hello");
            assertNull(repository.findByAAndB(1, "hello"));
            assertNotNull(repository.findByAAndB(0, "hello"));
            assertNotNull(repository.findByAAndB(2, "hello"));
        });
    }

    @TestFactory
    Stream<DynamicTest> deleteNonUnique() {
        return context.allTypesFor(DeleteRepository.class, repository -> {
            repository.insert(new TestEntity(0, "hello", "world!", null));
            repository.insert(new TestEntity(1, "hello", "world!", null));
            repository.insert(new TestEntity(2, "hello", "world!", null));

            assertNotNull(repository.findByAAndB(0, "hello"));
            assertNotNull(repository.findByAAndB(1, "hello"));
            assertNotNull(repository.findByAAndB(2, "hello"));
            assertEquals(3, repository.deleteByB("hello"));
            assertNull(repository.findByAAndB(0, "hello"));
            assertNull(repository.findByAAndB(1, "hello"));
            assertNull(repository.findByAAndB(2, "hello"));
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

                    assertNotNull(repository.findByAAndB(1, "hello"));
                    assertEquals(1, repository.deleteFirstByB("hello"));
                    assertNull(repository.findByAAndB(0, "hello"));
                    assertNotNull(repository.findByAAndB(1, "hello"));
                    assertNotNull(repository.findByAAndB(2, "hello"));
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

                    assertNotNull(repository.findByAAndB(1, "hello"));
                    var returning = repository.deleteReturning(1, "hello");
                    assertNotNull(returning);
                    assertEquals(new TestEntity(1, "hello", "world!", null), returning);

                    assertNull(repository.findByAAndB(1, "hello"));
                    assertNotNull(repository.findByAAndB(0, "hello"));
                    assertNotNull(repository.findByAAndB(2, "hello"));
                },
                DatabaseType.H2,
                DatabaseType.MYSQL,
                DatabaseType.ORACLE_DATABASE);
        // see readme why those are excluded for now
    }
}
