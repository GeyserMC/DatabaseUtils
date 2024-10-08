/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.delete;

import java.util.List;
import org.geysermc.databaseutils.IRepository;
import org.geysermc.databaseutils.ReusableTestRepository;
import org.geysermc.databaseutils.entity.TestEntity;
import org.geysermc.databaseutils.meta.Query;
import org.geysermc.databaseutils.meta.Repository;

@Repository
public interface DeleteRepository extends IRepository<TestEntity>, ReusableTestRepository {
    void insert(TestEntity entity);

    boolean existsByAAndB(int a, String b);

    void delete(TestEntity entity);

    void deleteByAAndB(int a, String b);

    @Query("deleteByAAndB")
    TestEntity deleteReturning(int a, String b);

    @Query("deleteByB")
    List<TestEntity> deleteReturningList(String b);

    int deleteByB(String b);

    int deleteFirstByB(String b);

    int deleteFirstByBOrderByA(String b);

    @Query("deleteFirstByBOrderByA")
    TestEntity deleteFirstWithOrderReturning(String b);

    @Override
    void delete();
}
