/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.codec;

import java.util.ArrayList;
import java.util.List;

public final class TypeCodecRegistry {
    private final List<TypeCodec<?>> codecs = new ArrayList<>();

    public TypeCodecRegistry() {
        codecs.add(UuidCodec.INSTANCE);
    }

    public TypeCodecRegistry(TypeCodecRegistry registry) {
        this();
        codecs.addAll(registry.codecs);
    }

    public void addCodec(TypeCodec<?> codec) {
        codecs.add(codec);
    }

    public <T> TypeCodec<T> codecFor(Class<T> type) {
        for (TypeCodec<?> codec : codecs) {
            if (codec.type(type)) {
                //noinspection unchecked
                return (TypeCodec<T>) codec;
            }
        }
        return null;
    }

    public <T> TypeCodec<T> requireCodecFor(Class<T> type) {
        var codec = codecFor(type);
        if (codec == null) {
            throw new IllegalStateException("Was not able to find codec for " + type.getName());
        }
        return codec;
    }

    public List<TypeCodec<?>> typeCodecs() {
        return codecs;
    }
}
