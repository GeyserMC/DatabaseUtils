package test.basic;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.geysermc.databaseutils.IRepository;
import org.geysermc.databaseutils.meta.Repository;

@Repository
public interface BasicRepository extends IRepository<TestEntity> {
    CompletableFuture<List<TestEntity>> find();

    CompletableFuture<TestEntity> findByAAndB(int aa, String b);

    Set<TestEntity> find(Set<TestEntity> entities);

    CompletableFuture<Boolean> exists();

    CompletableFuture<Boolean> existsByAOrB(int a, String bb);

    void update(List<TestEntity> entity);

    void update(TestEntity entity);

    CompletableFuture<Void> insert(TestEntity entity);

    void insert(List<TestEntity> entities);

    CompletableFuture<Void> delete(TestEntity entity);

    void delete(List<TestEntity> entities);

    CompletableFuture<Void> deleteByAAndB(int a, String b);
}