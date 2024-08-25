/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.mongo;

import static org.geysermc.databaseutils.util.ClassUtils.staticCastedValue;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import java.util.ArrayList;
import java.util.Objects;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.geysermc.databaseutils.Database;
import org.geysermc.databaseutils.DatabaseContext;
import org.geysermc.databaseutils.codec.TypeCodec;

public final class MongodbDatabase extends Database {
    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;

    @Override
    public void start(DatabaseContext context, Class<?> databaseImpl) {
        super.start(context, databaseImpl);
        var config = context.config();

        var connectionString = new ConnectionString(config.url());
        Objects.requireNonNull(connectionString.getDatabase(), "Database has to be specified!");

        var settings = MongoClientSettings.builder().applyConnectionString(connectionString);

        if (connectionString.getCredential() == null && (config.username() != null || config.password() != null)) {
            settings.credential(MongoCredential.createCredential(
                    config.username() != null ? config.username() : "",
                    connectionString.getDatabase(),
                    config.password() != null ? config.password().toCharArray() : new char[0]));
        }

        settings.codecRegistry(CodecRegistries.fromRegistries(
                entityCodecRegistry(databaseImpl),
                customCodecRegistry(context),
                MongoClientSettings.getDefaultCodecRegistry()));

        this.mongoClient = MongoClients.create(settings.build());
        this.mongoDatabase = mongoClient.getDatabase(connectionString.getDatabase());
    }

    @Override
    public void stop() {
        mongoClient.close();
    }

    public MongoClient mongoClient() {
        return mongoClient;
    }

    public MongoDatabase mongoDatabase() {
        return mongoDatabase;
    }

    @SuppressWarnings("unchecked")
    private CodecRegistry customCodecRegistry(DatabaseContext context) {
        var codecs = new ArrayList<Codec<?>>();
        for (TypeCodec<?> codec : context.registry().typeCodecs()) {
            codecs.add(new CustomTypeCodec((TypeCodec<Object>) codec));
        }
        return CodecRegistries.fromCodecs(codecs);
    }

    private CodecRegistry entityCodecRegistry(Class<?> databaseImpl) {
        try {
            return CodecRegistries.fromProviders(
                    staticCastedValue(databaseImpl.getDeclaredField("ENTITY_CODECS"), CodecProvider.class));
        } catch (NoSuchFieldException exception) {
            throw new RuntimeException("Expected there to be codecs for the entities!", exception);
        }
    }
}
