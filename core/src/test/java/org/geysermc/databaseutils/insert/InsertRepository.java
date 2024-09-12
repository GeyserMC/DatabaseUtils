/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.insert;

import java.util.List;
import org.geysermc.databaseutils.IRepository;
import org.geysermc.databaseutils.ReusableTestRepository;
import org.geysermc.databaseutils.entity.TestEntity;
import org.geysermc.databaseutils.meta.Query;
import org.geysermc.databaseutils.meta.Repository;

@Repository
public interface InsertRepository extends IRepository<TestEntity>, ReusableTestRepository {
    void insert(TestEntity entity);

    void insert(List<TestEntity> entities);

    @Query("insert")
    int insertWithCount(TestEntity entity);

    @Query("insert")
    int insertWithCount(List<TestEntity> entities);

    boolean existsByAAndB(int a, String b);

    @Override
    void delete();
}
