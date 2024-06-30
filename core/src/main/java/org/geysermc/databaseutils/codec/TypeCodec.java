/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.codec;

public interface TypeCodec<T> {
    Class<T> type();

    default boolean type(Class<?> type) {
        return type() == type;
    }

    T decode(byte[] input);

    byte[] encode(T input);
}
