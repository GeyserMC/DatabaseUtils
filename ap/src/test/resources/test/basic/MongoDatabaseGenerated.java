package org.geysermc.databaseutils.mongo;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import org.bson.BsonReader;
import org.bson.BsonSerializationException;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.geysermc.databaseutils.IRepository;
import org.geysermc.databaseutils.codec.TypeCodecRegistry;
import org.geysermc.databaseutils.util.TypedMap;
import test.basic.BasicRepositoryMongoImpl;
import test.basic.TestEntity;

class MongoDatabaseGenerated {
    private static final boolean HAS_ASYNC = true;

    private static final CodecProvider ENTITY_CODECS = new EntityCodecProvider();

    private static final List<BiFunction<MongodbDatabase, TypeCodecRegistry, IRepository<?>>> REPOSITORIES;

    static {
        REPOSITORIES = new ArrayList<>();
        REPOSITORIES.add(BasicRepositoryMongoImpl::new);
    }

    static void createEntities(MongodbDatabase database) throws MongoException {
        MongoDatabase mongoDatabase = database.mongoDatabase();
        var collectionNames = mongoDatabase.listCollectionNames().into(new ArrayList<>());
        if (!collectionNames.contains("hello")) {
            mongoDatabase.createCollection("hello");
            MongoCollection<Document> collection = mongoDatabase.getCollection("hello");
            collection.createIndex(new Document().append("c", 1));
            collection.createIndex(new Document().append("a", 1).append("b", 1), new IndexOptions().unique(true));
        }
    }

    private static final class TestEntityCodec implements Codec<TestEntity> {
        private final Codec<String> b;

        private final Codec<String> c;

        private final Codec<UUID> d;

        public TestEntityCodec(CodecRegistry registry) {
            this.b = registry.get(String.class);
            this.c = registry.get(String.class);
            this.d = registry.get(UUID.class);
        }

        @Override
        public TestEntity decode(BsonReader reader, DecoderContext context) {
            reader.readStartDocument();
            var map = new TypedMap();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                var name = reader.readName();
                if ("a".equals(name)) {
                    map.put("a", reader.readInt32());
                    continue;
                }
                if ("b".equals(name)) {
                    map.put("b", this.b.decode(reader, context));
                    continue;
                }
                if ("c".equals(name)) {
                    map.put("c", this.c.decode(reader, context));
                    continue;
                }
                if ("d".equals(name)) {
                    map.put("d", this.d.decode(reader, context));
                    continue;
                }
                if (!"_id".equals(name)) {
                    throw new BsonSerializationException("Unknown field %s".formatted(name));
                }
                reader.readObjectId();
            }
            reader.readEndDocument();
            return new TestEntity(map.get("a"), map.get("b"), map.get("c"), map.get("d"));
        }

        @Override
        public void encode(BsonWriter writer, TestEntity value, EncoderContext context) {
            writer.writeStartDocument();
            writer.writeName("a");
            writer.writeInt32(value.a());
            writer.writeName("b");
            this.b.encode(writer, value.b(), context);
            writer.writeName("c");
            this.c.encode(writer, value.c(), context);
            writer.writeName("d");
            this.d.encode(writer, value.d(), context);
            writer.writeEndDocument();
        }

        @Override
        public Class<TestEntity> getEncoderClass() {
            return TestEntity.class;
        }
    }

    private static final class EntityCodecProvider implements CodecProvider {
        @Override
        @SuppressWarnings({"unchecked"})
        public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
            if (clazz == TestEntity.class) {
                return (Codec<T>) new TestEntityCodec(registry);
            }
            return null;
        }
    }
}