/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.oracle.OracleContainer;

public final class TestContext {
    @SuppressWarnings("resource")
    private final Map<DatabaseType, ? extends GenericContainer<?>> containers = new HashMap<>() {
        {
            put(DatabaseType.MARIADB, new MariaDBContainer<>("mariadb:11.5"));
            put(DatabaseType.MYSQL, new MySQLContainer<>("mysql:9.0.1"));
            put(DatabaseType.MONGODB, new MongoDBContainer("mongo:7.0.14"));
            put(DatabaseType.POSTGRESQL, new PostgreSQLContainer<>("postgres:16.4"));
            // todo 'create table if not exists' is not a thing in mssqlserver
            // put(DatabaseType.SQL_SERVER, new MSSQLServerContainer<>(MSSQLServerContainer.IMAGE +
            // ":2022-latest").acceptLicense());
            put(DatabaseType.ORACLE_DATABASE, new OracleContainer("gvenzl/oracle-free:23.5-slim-faststart"));
        }
    };

    private final Map<DatabaseType, Map<Class<?>, IRepository<?>>> repositoriesForType = new ConcurrentHashMap<>();
    private final Set<Class<?>> usedRepositories = new HashSet<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Set<DatabaseType> limitedSet;

    public TestContext(DatabaseType... limitedSet) {
        this.limitedSet = new HashSet<>(Arrays.asList(limitedSet));
    }

    public TestContext() {
        this(DatabaseType.values());
    }

    @SafeVarargs
    public final void start(Class<? extends IRepository<?>>... repositories) {
        start(Arrays.asList(repositories));
    }

    public void start(List<Class<? extends IRepository<?>>> repositoryClasses) {
        // let's start all the databases at the same time, instead of waiting for each one to be ready
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (var entry : containers.entrySet()) {
            if (!limitedSet.contains(entry.getKey())) {
                continue;
            }
            futures.add(CompletableFuture.runAsync(() -> {
                var type = entry.getKey();
                var container = entry.getValue();

                container.start();

                String uri, username = null, password = null;
                if (container instanceof JdbcDatabaseContainer<?> jdbcContainer) {
                    uri = jdbcContainer.getJdbcUrl();
                    username = jdbcContainer.getUsername();
                    password = jdbcContainer.getPassword();
                } else if (container instanceof MongoDBContainer mongoContainer) {
                    uri = mongoContainer.getConnectionString() + "/database";
                } else {
                    throw new RuntimeException("Unknown container type: " + type);
                }

                addRepositoriesFor(type, uri, username, password, repositoryClasses);
            }));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();

        if (limitedSet.contains(DatabaseType.H2)) {
            addRepositoriesFor(DatabaseType.H2, null, null, null, repositoryClasses);
        }
        if (limitedSet.contains(DatabaseType.SQLITE)) {
            addRepositoriesFor(DatabaseType.SQLITE, null, null, null, repositoryClasses);
        }

        // ensure all database types are present
        // if (repositoriesForType.size() != DatabaseType.VALUES.length - limitedSet.size()) {
        //     throw new RuntimeException("Invalid number of repositories: " + repositoriesForType.size());
        // }
    }

    public void stop() {
        containers.values().forEach(GenericContainer::stop);
    }

    @SuppressWarnings("unchecked")
    public <T extends GenericContainer<?>> T container(DatabaseType type) {
        return (T) containers.get(type);
    }

    @SuppressWarnings("unchecked")
    public <T extends IRepository<?>> T repository(DatabaseType type, Class<T> repositoryClass) {
        T repo = (T) repositoriesForType.get(type).get(repositoryClass);
        if (repo != null) {
            usedRepositories.add(repositoryClass);
        }
        return repo;
    }

    public <T extends IRepository<?>> Stream<DynamicTest> allTypesFor(Class<T> repositoryClass, Consumer<T> callback) {
        return allTypesForBut(repositoryClass, callback);
    }

    @SuppressWarnings("unchecked")
    public <T extends IRepository<?>> Stream<DynamicTest> allTypesForBut(
            Class<T> repositoryClass, Consumer<T> callback, DatabaseType... excludedTypes) {
        var excluded = Arrays.asList(excludedTypes);
        var builder = Stream.<DynamicTest>builder();

        repositoriesForType.forEach((type, value) -> {
            if (excluded.contains(type)) {
                return;
            }
            var repository = (T) value.get(repositoryClass);
            if (repository != null) {
                usedRepositories.add(repositoryClass);
                builder.add(DynamicTest.dynamicTest(type.toString(), () -> callback.accept(repository)));
            }
        });
        return builder.build();
    }

    private void addRepositoriesFor(
            DatabaseType type,
            String uri,
            String username,
            String password,
            List<Class<? extends IRepository<?>>> repositoryClasses) {
        var repositories = repositoriesForType.computeIfAbsent(type, k -> new HashMap<>());

        var instance = DatabaseUtils.builder()
                .type(type)
                .uri(uri)
                .username(username)
                .password(password)
                .executorService(executor)
                .build();
        for (IRepository<?> repository : instance.start()) {
            for (var repositoryClass : repositoryClasses) {
                if (repositoryClass.isInstance(repository)) {
                    repositories.put(repositoryClass, repository);
                }
            }
        }
    }

    public void deleteRows() {
        for (Class<?> usedRepository : usedRepositories) {
            for (var repositories : repositoriesForType.values()) {
                var repoInstance = repositories.get(usedRepository);
                if (repoInstance instanceof ReusableTestRepository reusable) {
                    reusable.delete();
                } else {
                    throw new RuntimeException(
                            "Expected all repositories to be reusable, but %s is not".formatted(usedRepository));
                }
            }
        }
        usedRepositories.clear();
    }
}
