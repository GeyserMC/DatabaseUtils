/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.mongo;

import org.bson.BsonBinary;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.geysermc.databaseutils.codec.TypeCodec;

public final class CustomTypeCodec implements Codec<Object> {
    private final TypeCodec<Object> codec;

    public CustomTypeCodec(TypeCodec<Object> codec) {
        this.codec = codec;
    }

    @Override
    public Object decode(BsonReader bsonReader, DecoderContext decoderContext) {
        return codec.decode(bsonReader.readBinaryData().getData());
    }

    @Override
    public void encode(BsonWriter bsonWriter, Object value, EncoderContext encoderContext) {
        bsonWriter.writeBinaryData(new BsonBinary(codec.encode(value)));
    }

    @Override
    public Class<Object> getEncoderClass() {
        return codec.type();
    }
}
