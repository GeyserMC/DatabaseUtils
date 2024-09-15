package test.advanced;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import java.lang.Boolean;
import java.lang.Exception;
import java.lang.IllegalStateException;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bson.conversions.Bson;
import org.geysermc.databaseutils.codec.TypeCodec;
import org.geysermc.databaseutils.codec.TypeCodecRegistry;
import org.geysermc.databaseutils.mongo.MongodbDatabase;

public final class AdvancedRepositoryMongoImpl implements AdvancedRepository {
    private final MongodbDatabase database;
    private final MongoClient mongoClient;
    private final MongoCollection<TestEntity> collection;
    private final TypeCodec<UUID> __d;

    public AdvancedRepositoryMongoImpl(MongodbDatabase database, TypeCodecRegistry registry) {
        this.database = database;
        this.mongoClient = database.mongoClient();
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
    public List<String> findTop3BByA(int a) {
        return this.collection.find(Filters.eq("a", a)).limit(3).map(TestEntity::b).into(new ArrayList<>());
    }

    @Override
    public CompletableFuture<Boolean> existsByAOrB(int a, String bb) {
        return CompletableFuture.supplyAsync(() -> {
            return this.collection.find(Filters.or(Filters.eq("a", a), Filters.eq("b", bb))).limit(1).first() != null;
        } , this.database.executorService());
    }

    @Override
    public void updateByBAndC(String b, String oldC, String c) {
        this.collection.updateMany(Filters.and(Filters.eq("b", b), Filters.eq("c", oldC)), Updates.combine(Updates.set("c", c)));
    }

    @Override
    public CompletableFuture<Boolean> deleteByAAndBAndC(int a, String b, String c) {
        return CompletableFuture.supplyAsync(() -> {
            long __count;
            __count = (long) this.collection.deleteMany(Filters.and(Filters.eq("a", a), Filters.and(Filters.eq("b", b), Filters.eq("c", c)))).getDeletedCount();
            return __count > 0;
        } , this.database.executorService());
    }

    @Override
    public int deleteByAAndC(int a, String c) {
        int __count;
        __count = (int) this.collection.deleteMany(Filters.and(Filters.eq("a", a), Filters.eq("c", c))).getDeletedCount();
        return __count;
    }

    @Override
    public TestEntity deleteByAAndB(int a, String b) {
        return this.collection.findOneAndDelete(Filters.and(Filters.eq("a", a), Filters.eq("b", b)));
    }

    @Override
    public List<TestEntity> deleteByBAndC(String b, String c) {
        var __session = this.mongoClient.startSession();
        try {
            __session.startTransaction();
            var __find = this.collection.find(__session, Filters.and(Filters.eq("b", b), Filters.eq("c", c))).into(new ArrayList<>());
            var __toDelete = new ArrayList<Bson>();
            for (var __found : __find) {
                __toDelete.add(Filters.and(Filters.eq("a", __found.a()), Filters.eq("b", __found.b())));
            }
            var __deletedCount = this.collection.deleteMany(__session, Filters.or(__toDelete)).getDeletedCount();
            if (__find.size() != __deletedCount) {
                throw new IllegalStateException("Found %s documents but deleted %s documents".formatted(__find.size(), __deletedCount));
            }
            __session.commitTransaction();
            return __find;
        } catch (Exception __exception) {
            __session.abortTransaction();
            throw __exception;
        } finally {
            __session.close();
        }
    }

    @Override
    public TestEntity findWithAlternativeName(int a, String b) {
        return this.collection.find(Filters.and(Filters.eq("a", a), Filters.and(Filters.eq("b", b), Filters.not(Filters.eq("c", null))))).first();
    }
}