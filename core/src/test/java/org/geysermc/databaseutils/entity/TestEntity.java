/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.entity;

import java.util.UUID;
import org.geysermc.databaseutils.meta.Entity;
import org.geysermc.databaseutils.meta.Index;
import org.geysermc.databaseutils.meta.Key;
import org.geysermc.databaseutils.meta.Length;

@Index(columns = {"c"})
@Entity("hello")
public record TestEntity(
        @Key int a, @Key @Length(max = 50) String b, @Length(max = 30) String c, @Length(max = 16) UUID d) {}
