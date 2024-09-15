/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
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

final class UpdateTests {
    static TestContext context = TestContext.INSTANCE;

    @BeforeAll
    static void setUp() {
        context.start(UpdateRepository.class);
    }

    @AfterAll
    static void tearDown() {
        context.stop();
    }

    @AfterEach
    void clenUp() {
        context.deleteRows();
    }

    // todo determine how we should handle duplicated keys

    @TestFactory
    Stream<DynamicTest> updateSingle() {
        return context.allTypesFor(UpdateRepository.class, repository -> {
            repository.insert(new TestEntity(0, "hello", "world!", null));
            repository.insert(new TestEntity(1, "hello", "world!", null));
            repository.insert(new TestEntity(2, "hello", "world!", null));

            repository.update(new TestEntity(0, "hello", "steve!", null));
            assertEquals(new TestEntity(0, "hello", "steve!", null), repository.findByAAndB(0, "hello"));
            assertEquals(new TestEntity(1, "hello", "world!", null), repository.findByAAndB(1, "hello"));
            assertEquals(new TestEntity(2, "hello", "world!", null), repository.findByAAndB(2, "hello"));
        });
    }

    @TestFactory
    Stream<DynamicTest> updateMany() {
        return context.allTypesFor(UpdateRepository.class, repository -> {
            repository.insert(new TestEntity(0, "hello", "world!", null));
            repository.insert(new TestEntity(1, "hello", "world!", null));
            repository.insert(new TestEntity(2, "hello", "steve!", null));

            repository.update(
                    List.of(new TestEntity(0, "hello", "alex!", null), new TestEntity(1, "hello", "alex!", null)));
            assertEquals(new TestEntity(0, "hello", "alex!", null), repository.findByAAndB(0, "hello"));
            assertEquals(new TestEntity(1, "hello", "alex!", null), repository.findByAAndB(1, "hello"));
            assertEquals(new TestEntity(2, "hello", "steve!", null), repository.findByAAndB(2, "hello"));

            repository.update(List.of(new TestEntity(0, "hello", "world!", null)));
            assertEquals(new TestEntity(0, "hello", "world!", null), repository.findByAAndB(0, "hello"));
            assertEquals(new TestEntity(1, "hello", "alex!", null), repository.findByAAndB(1, "hello"));
            assertEquals(new TestEntity(2, "hello", "steve!", null), repository.findByAAndB(2, "hello"));

            repository.update(List.of());
            assertEquals(new TestEntity(0, "hello", "world!", null), repository.findByAAndB(0, "hello"));
            assertEquals(new TestEntity(1, "hello", "alex!", null), repository.findByAAndB(1, "hello"));
            assertEquals(new TestEntity(2, "hello", "steve!", null), repository.findByAAndB(2, "hello"));
        });
    }

    @TestFactory
    Stream<DynamicTest> updateSingleWithCount() {
        return context.allTypesFor(UpdateRepository.class, repository -> {
            repository.insert(new TestEntity(0, "hello", "world!", null));
            repository.insert(new TestEntity(1, "hello", "world!", null));
            repository.insert(new TestEntity(2, "hello", "world!", null));

            assertEquals(1, repository.updateWithCount(new TestEntity(0, "hello", "steve!", null)));
            assertEquals(new TestEntity(0, "hello", "steve!", null), repository.findByAAndB(0, "hello"));
            assertEquals(new TestEntity(1, "hello", "world!", null), repository.findByAAndB(1, "hello"));
            assertEquals(new TestEntity(2, "hello", "world!", null), repository.findByAAndB(2, "hello"));
        });
    }

    @TestFactory
    Stream<DynamicTest> updateManyWithCount() {
        return context.allTypesFor(UpdateRepository.class, repository -> {
            repository.insert(new TestEntity(0, "hello", "world!", null));
            repository.insert(new TestEntity(1, "hello", "world!", null));
            repository.insert(new TestEntity(2, "hello", "steve!", null));

            assertEquals(
                    2,
                    repository.updateWithCount(List.of(
                            new TestEntity(0, "hello", "alex!", null), new TestEntity(1, "hello", "alex!", null))));
            assertEquals(new TestEntity(0, "hello", "alex!", null), repository.findByAAndB(0, "hello"));
            assertEquals(new TestEntity(1, "hello", "alex!", null), repository.findByAAndB(1, "hello"));
            assertEquals(new TestEntity(2, "hello", "steve!", null), repository.findByAAndB(2, "hello"));

            assertEquals(1, repository.updateWithCount(List.of(new TestEntity(0, "hello", "world!", null))));
            assertEquals(new TestEntity(0, "hello", "world!", null), repository.findByAAndB(0, "hello"));
            assertEquals(new TestEntity(1, "hello", "alex!", null), repository.findByAAndB(1, "hello"));
            assertEquals(new TestEntity(2, "hello", "steve!", null), repository.findByAAndB(2, "hello"));

            assertEquals(0, repository.updateWithCount(List.of()));
            assertEquals(new TestEntity(0, "hello", "world!", null), repository.findByAAndB(0, "hello"));
            assertEquals(new TestEntity(1, "hello", "alex!", null), repository.findByAAndB(1, "hello"));
            assertEquals(new TestEntity(2, "hello", "steve!", null), repository.findByAAndB(2, "hello"));
        });
    }

    @TestFactory
    Stream<DynamicTest> updateSingleEditKey() {
        return context.allTypesFor(UpdateRepository.class, repository -> {
            repository.insert(new TestEntity(0, "hello", "world!", null));
            repository.insert(new TestEntity(1, "hello", "world!", null));
            repository.insert(new TestEntity(2, "hello", "world!", null));

            // TestEntity has combined key of a and b, so b cannot be set like this as it doesn't match
            assertEquals(0, repository.updateWithCount(new TestEntity(0, "goodbye", "steve!", null)));
            assertEquals(new TestEntity(0, "hello", "world!", null), repository.findByAAndB(0, "hello"));
            assertEquals(new TestEntity(1, "hello", "world!", null), repository.findByAAndB(1, "hello"));
            assertEquals(new TestEntity(2, "hello", "world!", null), repository.findByAAndB(2, "hello"));
        });
    }

    @TestFactory
    Stream<DynamicTest> updateEditKey() {
        return context.allTypesFor(UpdateRepository.class, repository -> {
            repository.insert(new TestEntity(0, "hello", "steve!", null));
            repository.insert(new TestEntity(1, "hello", "world!", null));
            repository.insert(new TestEntity(2, "hello", "world!", null));

            assertEquals(1, repository.updateByBAndC("hello", "steve!", 3));
            assertNull(repository.findByAAndB(0, "hello"));
            assertEquals(new TestEntity(1, "hello", "world!", null), repository.findByAAndB(1, "hello"));
            assertEquals(new TestEntity(2, "hello", "world!", null), repository.findByAAndB(2, "hello"));
            assertEquals(new TestEntity(3, "hello", "steve!", null), repository.findByAAndB(3, "hello"));
        });
    }

    @TestFactory
    Stream<DynamicTest> updateEditKeyDuplicate() {
        // todo add unique index for key for sql types
        return context.allTypesForOnly(
                UpdateRepository.class,
                repository -> {
                    repository.insert(new TestEntity(0, "hello", "steve!", null));
                    repository.insert(new TestEntity(1, "hello", "world!", null));
                    repository.insert(new TestEntity(2, "hello", "world!", null));

                    // todo properly handle it with own exception types
                    // and have a per repo/method option to not throw
                    assertThrows(Exception.class, () -> repository.updateByBAndC("hello", "steve!", 1));
                    assertEquals(new TestEntity(0, "hello", "steve!", null), repository.findByAAndB(0, "hello"));
                    assertEquals(new TestEntity(1, "hello", "world!", null), repository.findByAAndB(1, "hello"));
                    assertEquals(new TestEntity(2, "hello", "world!", null), repository.findByAAndB(2, "hello"));
                },
                DatabaseType.MONGODB);
    }

    @TestFactory
    Stream<DynamicTest> updateOneColumn() {
        return context.allTypesFor(UpdateRepository.class, repository -> {
            repository.insert(new TestEntity(0, "hello", "world!", null));
            repository.insert(new TestEntity(1, "hello", "steve!", null));
            repository.insert(new TestEntity(2, "hello", "world!", null));

            assertEquals(2, repository.updateByBAndC("hello", "world!", "alex!"));
            assertEquals(new TestEntity(0, "hello", "alex!", null), repository.findByAAndB(0, "hello"));
            assertEquals(new TestEntity(1, "hello", "steve!", null), repository.findByAAndB(1, "hello"));
            assertEquals(new TestEntity(2, "hello", "alex!", null), repository.findByAAndB(2, "hello"));

            assertEquals(1, repository.updateByBAndC("hello", "steve!", "world!"));
            assertEquals(new TestEntity(0, "hello", "alex!", null), repository.findByAAndB(0, "hello"));
            assertEquals(new TestEntity(1, "hello", "world!", null), repository.findByAAndB(1, "hello"));
            assertEquals(new TestEntity(2, "hello", "alex!", null), repository.findByAAndB(2, "hello"));
        });
    }

    @TestFactory
    Stream<DynamicTest> updateTwoColumns() {
        return context.allTypesFor(UpdateRepository.class, repository -> {
            repository.insert(new TestEntity(0, "hello", "world!", null));
            repository.insert(new TestEntity(1, "hello", "steve!", null));
            repository.insert(new TestEntity(2, "hello", "world!", null));

            assertEquals(2, repository.updateByBAndC("hello", "world!", "alex!", fromName("alex")));
            assertEquals(new TestEntity(0, "hello", "alex!", fromName("alex")), repository.findByAAndB(0, "hello"));
            assertEquals(new TestEntity(1, "hello", "steve!", null), repository.findByAAndB(1, "hello"));
            assertEquals(new TestEntity(2, "hello", "alex!", fromName("alex")), repository.findByAAndB(2, "hello"));

            assertEquals(1, repository.updateByBAndC("hello", "steve!", "world!", fromName("world")));
            assertEquals(new TestEntity(0, "hello", "alex!", fromName("alex")), repository.findByAAndB(0, "hello"));
            assertEquals(new TestEntity(1, "hello", "world!", fromName("world")), repository.findByAAndB(1, "hello"));
            assertEquals(new TestEntity(2, "hello", "alex!", fromName("alex")), repository.findByAAndB(2, "hello"));
        });
    }

    private UUID fromName(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
    }
}
