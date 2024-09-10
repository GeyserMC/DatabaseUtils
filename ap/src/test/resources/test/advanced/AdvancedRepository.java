package test.advanced;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.geysermc.databaseutils.IRepository;
import org.geysermc.databaseutils.meta.Repository;

@Repository
public interface AdvancedRepository extends IRepository<TestEntity> {
    CompletableFuture<TestEntity> findByAAndB(int aa, String b);

    List<String> findTop3BByA(int a);

    CompletableFuture<Boolean> existsByAOrB(int a, String bb);

    void updateByBAndC(String b, String oldC, String c);

    CompletableFuture<Boolean> deleteByAAndBAndC(int a, String b, String c);

    int deleteByAAndC(int a, String c);

    TestEntity deleteByAAndB(int a, String b);

    List<TestEntity> deleteByBAndC(String b, String c);

//    int deleteAByBAndC(String b, String c); todo support this
}