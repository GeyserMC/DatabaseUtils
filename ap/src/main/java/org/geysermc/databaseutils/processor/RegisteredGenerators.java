/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.geysermc.databaseutils.DatabaseCategory;
import org.geysermc.databaseutils.processor.type.DatabaseGenerator;
import org.geysermc.databaseutils.processor.type.RepositoryGenerator;
import org.geysermc.databaseutils.processor.type.mongo.MongoDatabaseGenerator;
import org.geysermc.databaseutils.processor.type.mongo.MongoRepositoryGenerator;
import org.geysermc.databaseutils.processor.type.sql.SqlDatabaseGenerator;
import org.geysermc.databaseutils.processor.type.sql.SqlRepositoryGenerator;

final class RegisteredGenerators {
    private static final Map<DatabaseCategory, Supplier<DatabaseGenerator>> DATABASE_GENERATORS = new HashMap<>();
    private static final Map<DatabaseCategory, Supplier<RepositoryGenerator>> REPOSITORY_GENERATORS = new HashMap<>();

    private RegisteredGenerators() {}

    public static List<DatabaseGenerator> databaseGenerators() {
        return DATABASE_GENERATORS.values().stream().map(Supplier::get).collect(Collectors.toList());
    }

    public static List<RepositoryGenerator> repositoryGenerators() {
        return REPOSITORY_GENERATORS.values().stream().map(Supplier::get).collect(Collectors.toList());
    }

    public static int generatorCount() {
        return DATABASE_GENERATORS.size();
    }

    static {
        // todo make it less cursed by using one map/list with everything for each database category
        DATABASE_GENERATORS.put(DatabaseCategory.SQL, SqlDatabaseGenerator::new);
        DATABASE_GENERATORS.put(DatabaseCategory.MONGODB, MongoDatabaseGenerator::new);

        REPOSITORY_GENERATORS.put(DatabaseCategory.SQL, SqlRepositoryGenerator::new);
        REPOSITORY_GENERATORS.put(DatabaseCategory.MONGODB, MongoRepositoryGenerator::new);
    }
}
