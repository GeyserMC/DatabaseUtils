/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.delete.repository;

import org.geysermc.databaseutils.IRepository;
import org.geysermc.databaseutils.ReusableTestRepository;
import org.geysermc.databaseutils.entity.TestEntity;
import org.geysermc.databaseutils.meta.Query;
import org.geysermc.databaseutils.meta.Repository;

@Repository
public interface DeleteRepository extends IRepository<TestEntity>, ReusableTestRepository {
    void insert(TestEntity entity);

    TestEntity findByAAndB(int a, String b);

    void delete(TestEntity entity);

    void deleteByAAndB(int a, String b);

    @Query("deleteByAAndB")
    TestEntity deleteReturning(int a, String b);

    int deleteByB(String b);

    int deleteFirstByB(String b);

    @Override
    void delete();
}
