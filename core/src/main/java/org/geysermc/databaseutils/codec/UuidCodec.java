/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.codec;

import java.nio.ByteBuffer;
import java.util.UUID;
import org.geysermc.databaseutils.meta.Length;

@Length(max = 16)
final class UuidCodec implements TypeCodec<UUID> {
    static final UuidCodec INSTANCE = new UuidCodec();

    private UuidCodec() {}

    @Override
    public Class<UUID> type() {
        return UUID.class;
    }

    @Override
    public UUID decode(byte[] input) {
        // todo document that (at least in mongodb) it is guaranteed null
        if (input == null) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap(input);
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    @Override
    public byte[] encode(UUID input) {
        // todo decide whether or not this behaviour should be common
        if (input == null) {
            return null;
        }

        byte[] uuidBytes = new byte[16];
        ByteBuffer.wrap(uuidBytes).putLong(input.getMostSignificantBits()).putLong(input.getLeastSignificantBits());
        return uuidBytes;
    }
}
