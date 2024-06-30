/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.codec;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

final class UuidCodec implements TypeCodec<UUID> {
    static final UuidCodec INSTANCE = new UuidCodec();

    private UuidCodec() {}

    @Override
    public Class<UUID> type() {
        return UUID.class;
    }

    @Override
    public UUID decode(byte[] input) {
        ByteBuffer buffer = ByteBuffer.wrap(input);
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    @Override
    public byte[] encode(UUID input) {
        byte[] uuidBytes = new byte[16];
        ByteBuffer.wrap(uuidBytes)
                .order(ByteOrder.BIG_ENDIAN)
                .putLong(input.getMostSignificantBits())
                .putLong(input.getLeastSignificantBits());
        return uuidBytes;
    }
}
