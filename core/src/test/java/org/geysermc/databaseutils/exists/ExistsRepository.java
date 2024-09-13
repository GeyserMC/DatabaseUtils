/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.exists;

import org.geysermc.databaseutils.IRepository;
import org.geysermc.databaseutils.ReusableTestRepository;
import org.geysermc.databaseutils.entity.TestEntity;
import org.geysermc.databaseutils.meta.Repository;

@Repository
public interface ExistsRepository extends IRepository<TestEntity>, ReusableTestRepository {
    boolean exists(TestEntity entity);

    void insert(TestEntity entity);

    @Override
    void delete();
}
