/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils;

import java.util.concurrent.ExecutorService;
import org.geysermc.databaseutils.codec.TypeCodecRegistry;

public record DatabaseContext(
        DatabaseConfig config,
        String poolName,
        DatabaseType type,
        ExecutorService service,
        TypeCodecRegistry registry) {

    public DatabaseContext {
        if (poolName == null || poolName.isEmpty())
            throw new IllegalArgumentException("poolName cannot be null or empty");
        if (type == null) throw new IllegalArgumentException("A database type has to be provided");
    }

    public DatabaseContext(
            String url,
            String username,
            String password,
            int connectionPoolSize,
            String poolName,
            DatabaseType type,
            ExecutorService service,
            TypeCodecRegistry registry) {
        this(new DatabaseConfig(url, username, password, connectionPoolSize), poolName, type, service, registry);
    }
}
