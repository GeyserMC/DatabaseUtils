/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils;

import static org.geysermc.databaseutils.util.ClassUtils.access;
import static org.geysermc.databaseutils.util.ClassUtils.staticCastedValue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.databaseutils.codec.TypeCodecRegistry;

final class DatabaseLoader {
    @NonNull StartResult startDatabase(DatabaseContext context) {
        var database = DatabaseRegistry.databaseFor(context.type());
        if (database == null) {
            throw new IllegalStateException("Couldn't find a database manager for " + context.type());
        }

        Class<?> databaseImplClass;
        boolean hasAsync;
        List<BiFunction<Database, TypeCodecRegistry, IRepository<?>>> repositoryCreators;
        Method createEntitiesMethod;
        try {
            var className = context.type().databaseCategory().upperCamelCaseName() + "DatabaseGenerated";
            databaseImplClass = Class.forName(database.getClass().getPackageName() + "." + className);

            hasAsync = access(databaseImplClass.getDeclaredField("HAS_ASYNC")).getBoolean(null);
            repositoryCreators = staticCastedValue(databaseImplClass.getDeclaredField("REPOSITORIES"));
            createEntitiesMethod = access(databaseImplClass.getDeclaredMethod("createEntities", database.getClass()));
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Could not find database implementation!", exception);
        } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException exception) {
            throw new RuntimeException("Something went wrong with the generated database implementation", exception);
        }

        if (hasAsync && context.service() == null) {
            throw new IllegalStateException("Database has async methods but no ExecutorService was provided!");
        }

        database.start(context, databaseImplClass);

        try {
            createEntitiesMethod.invoke(null, database);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new RuntimeException("Something went wrong with creating entities", exception);
        }

        var repositories = new ArrayList<IRepository<?>>();
        for (var repositoryCreator : repositoryCreators) {
            repositories.add(repositoryCreator.apply(database, context.registry()));
        }

        return new StartResult(database, repositories);
    }

    record StartResult(Database database, List<IRepository<?>> repositories) {}
}
