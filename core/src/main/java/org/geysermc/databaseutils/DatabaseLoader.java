/*
 * Copyright (c) 2024 GeyserMC <https://geysermc.org>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.databaseutils.codec.TypeCodecRegistry;

final class DatabaseLoader {
    @SuppressWarnings({"unchecked"})
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
            databaseImplClass = Class.forName(database.getClass().getName() + "Generated");

            hasAsync = access(databaseImplClass.getDeclaredField("HAS_ASYNC")).getBoolean(null);
            repositoryCreators = (List<BiFunction<Database, TypeCodecRegistry, IRepository<?>>>)
                    access(databaseImplClass.getDeclaredField("REPOSITORIES")).get(null);
            createEntitiesMethod = access(databaseImplClass.getDeclaredMethod("createEntities", database.getClass()));
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Could not find database implementation!", exception);
        } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException exception) {
            throw new RuntimeException("Something went wrong with the generated database implementation", exception);
        }

        if (hasAsync && context.service() == null) {
            throw new IllegalStateException("Database has async methods but no ExecutorService was provided!");
        }

        database.start(context);

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

    private <T extends AccessibleObject> T access(T member) {
        member.setAccessible(true);
        return member;
    }

    record StartResult(Database database, List<IRepository<?>> repositories) {}
}
