/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.util;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class CollectionUtils {
    public static <I, O> List<O> map(Collection<I> items, Function<I, O> mapper) {
        return items.stream().map(mapper).toList();
    }

    public static <T> String mapAndJoin(Collection<T> items, Function<T, ?> mapper) {
        return join(map(items, mapper));
    }

    public static String join(Collection<?> items, int offset) {
        return join(items.stream().skip(offset).toList());
    }

    public static String join(Collection<?> items) {
        return String.join(", ", items.stream().map(Object::toString).toList());
    }
}
