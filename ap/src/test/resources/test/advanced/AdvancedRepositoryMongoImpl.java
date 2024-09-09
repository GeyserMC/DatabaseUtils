package test.advanced;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bson.Document;
import org.geysermc.databaseutils.codec.TypeCodec;
import org.geysermc.databaseutils.codec.TypeCodecRegistry;
import org.geysermc.databaseutils.mongo.MongodbDatabase;

public final class AdvancedRepositoryMongoImpl implements AdvancedRepository {
    private final MongodbDatabase database;

    private final MongoCollection<TestEntity> collection;

    private final TypeCodec<UUID> __d;

    public AdvancedRepositoryMongoImpl(MongodbDatabase database, TypeCodecRegistry registry) {
        this.database = database;
        this.collection = database.mongoDatabase().getCollection("hello", TestEntity.class);
        this.__d = registry.requireCodecFor(UUID.class);
    }

    @Override
    public CompletableFuture<TestEntity> findByAAndB(int aa, String b) {
        return CompletableFuture.supplyAsync(() -> {
            return this.collection.find(Filters.and(Filters.eq("a", aa), Filters.eq("b", b))).first();
        } , this.database.executorService());
    }

    @Override
    public CompletableFuture<Boolean> existsByAOrB(int a, String bb) {
        return CompletableFuture.supplyAsync(() -> {
            return this.collection.find(Filters.or(Filters.eq("a", a), Filters.eq("b", bb))).limit(1).first() != null;
        } , this.database.executorService());
    }

    @Override
    public void updateCByBAndC(String newValue, String b, String c) {
        this.collection.updateMany(Filters.and(Filters.eq("b", b), Filters.eq("c", c)), new Document("c", newValue));
    }

    @Override
    public CompletableFuture<Boolean> deleteByAAndB(int a, String b) {
        return CompletableFuture.supplyAsync(() -> {
            int __count;
            __count = (int) this.collection.deleteMany(Filters.and(Filters.eq("a", a), Filters.eq("b", b))).getDeletedCount();
            return __count > 0;
        } , this.database.executorService());
    }

    @Override
    public Integer deleteByAAndC(int a, String c) {
        int __count;
        __count = (int) this.collection.deleteMany(Filters.and(Filters.eq("a", a), Filters.eq("c", c))).getDeletedCount();
        return __count;
    }
}