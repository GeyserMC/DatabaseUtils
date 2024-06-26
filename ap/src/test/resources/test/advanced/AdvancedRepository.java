package test.advanced;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.geysermc.databaseutils.IRepository;
import org.geysermc.databaseutils.meta.Repository;

@Repository
public interface AdvancedRepository extends IRepository<TestEntity> {
    CompletableFuture<TestEntity> findByAAndB(int aa, String b);

    CompletableFuture<Boolean> existsByAOrB(int a, String bb);

    void updateCByBAndC(String newValue, String b, String c);
}