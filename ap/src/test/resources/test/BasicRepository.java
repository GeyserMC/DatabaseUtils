package test;

import java.util.concurrent.CompletableFuture;
import org.geysermc.databaseutils.IRepository;
import org.geysermc.databaseutils.meta.Repository;

@Repository
public interface BasicRepository extends IRepository<TestEntity> {
    CompletableFuture<TestEntity> findByAAndB(int a, String b);

    CompletableFuture<Boolean> existsByAOrB(int a, String b);
}