package test.basic;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.DeleteOneModel;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.WriteModel;
import java.lang.Boolean;
import java.lang.Override;
import java.lang.String;
import java.lang.Void;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.geysermc.databaseutils.codec.TypeCodec;
import org.geysermc.databaseutils.codec.TypeCodecRegistry;
import org.geysermc.databaseutils.mongo.MongodbDatabase;

public final class BasicRepositoryMongoImpl implements BasicRepository {
    private final MongodbDatabase database;

    private final MongoCollection<TestEntity> collection;

    private final TypeCodec<UUID> __d;

    public BasicRepositoryMongoImpl(MongodbDatabase database, TypeCodecRegistry registry) {
        this.database = database;
        this.collection = database.mongoDatabase().getCollection("hello", TestEntity.class);
        this.__d = registry.requireCodecFor(UUID.class);
    }

    @Override
    public CompletableFuture<List<TestEntity>> find() {
        return CompletableFuture.supplyAsync(() -> {
            return this.collection.find(Filters.empty()).into(new ArrayList<>());
        } , this.database.executorService());
    }

    @Override
    public CompletableFuture<TestEntity> findByA(int a) {
        return CompletableFuture.supplyAsync(() -> {
            return this.collection.find(Filters.eq("a", a)).first();
        } , this.database.executorService());
    }

    @Override
    public CompletableFuture<Boolean> exists() {
        return CompletableFuture.supplyAsync(() -> {
            return this.collection.find(Filters.empty()).limit(1).first() != null;
        } , this.database.executorService());
    }

    @Override
    public CompletableFuture<Boolean> existsByB(String b) {
        return CompletableFuture.supplyAsync(() -> {
            return this.collection.find(Filters.eq("b", b)).limit(1).first() != null;
        } , this.database.executorService());
    }

    @Override
    public void update(List<TestEntity> entity) {
        var __bulkOperations = new ArrayList<WriteModel<TestEntity>>();
        for (var __entry : entity) {
            __bulkOperations.add(new ReplaceOneModel<>(Filters.and(Filters.eq("a", __entry.a()), Filters.eq("b", __entry.b())), __entry));
        }
        this.collection.bulkWrite(__bulkOperations);
    }

    @Override
    public void update(TestEntity entity) {
        this.collection.replaceOne(Filters.and(Filters.eq("a", entity.a()), Filters.eq("b", entity.b())), entity);
    }

    @Override
    public CompletableFuture<Void> insert(TestEntity entity) {
        return CompletableFuture.supplyAsync(() -> {
            this.collection.insertOne(entity);
            return null;
        } , this.database.executorService());
    }

    @Override
    public void insert(List<TestEntity> entities) {
        this.collection.insertMany(entities);
    }

    @Override
    public CompletableFuture<Void> delete(TestEntity entity) {
        return CompletableFuture.supplyAsync(() -> {
            this.collection.deleteOne(Filters.and(Filters.eq("a", entity.a()), Filters.eq("b", entity.b())));
            return null;
        } , this.database.executorService());
    }

    @Override
    public void delete(List<TestEntity> entities) {
        var __bulkOperations = new ArrayList<WriteModel<TestEntity>>();
        for (var __entry : entities) {
            __bulkOperations.add(new DeleteOneModel<>(Filters.and(Filters.eq("a", __entry.a()), Filters.eq("b", __entry.b()))));
        }
        this.collection.bulkWrite(__bulkOperations);
    }

    @Override
    public CompletableFuture<Void> deleteByAAndB(int a, String b) {
        return CompletableFuture.supplyAsync(() -> {
            this.collection.deleteMany(Filters.and(Filters.eq("a", a), Filters.eq("b", b)));
            return null;
        } , this.database.executorService());
    }
}