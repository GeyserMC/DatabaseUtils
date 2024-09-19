/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class CollectionUtils {
    public static <I, O> List<O> map(Collection<I> items, Function<I, O> mapper) {
        return items.stream().map(mapper).toList();
    }

    @SuppressWarnings("unchecked")
    public static <I, O> O[] map(I[] items, Function<I, O> mapper) {
        O[] array = (O[]) Array.newInstance(items.getClass().getComponentType(), items.length);
        for (int i = 0; i < items.length; i++) {
            array[i] = mapper.apply(items[i]);
        }
        return array;
    }

    public static <T> String mapAndJoin(Collection<T> items, Function<T, ?> mapper) {
        return join(map(items, mapper));
    }

    public static <T> String mapAndJoin(Collection<T> items, Function<T, ?> mapper, String delimiter) {
        return join(map(items, mapper), delimiter);
    }

    public static String join(Collection<?> items, int offset) {
        return join(items.stream().skip(offset).toList());
    }

    public static String join(Collection<?> items) {
        return join(items, ", ");
    }

    public static String join(Collection<?> items, String delimiter) {
        return String.join(delimiter, items.stream().map(Object::toString).toList());
    }
}
