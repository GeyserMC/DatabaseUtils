/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.update;

import java.util.List;
import java.util.UUID;
import org.geysermc.databaseutils.IRepository;
import org.geysermc.databaseutils.ReusableTestRepository;
import org.geysermc.databaseutils.entity.TestEntity;
import org.geysermc.databaseutils.meta.Query;
import org.geysermc.databaseutils.meta.Repository;

@Repository
public interface UpdateRepository extends IRepository<TestEntity>, ReusableTestRepository {
    void update(TestEntity entity);

    void update(List<TestEntity> entities);

    @Query("update")
    int updateWithCount(TestEntity entity);

    @Query("update")
    int updateWithCount(List<TestEntity> entities);

    int updateByBAndC(String b, String oldC, String c);

    int updateByBAndC(String b, String oldC, int a);

    int updateByBAndC(String b, String oldC, String c, UUID d);

    void insert(TestEntity entity);

    TestEntity findByAAndB(int a, String b);

    @Override
    void delete();
}
