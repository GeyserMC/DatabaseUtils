/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils;

import java.util.concurrent.ExecutorService;

public abstract class Database {
    protected ExecutorService service;

    public void start(DatabaseContext context, Class<?> databaseImpl) {
        this.service = context.service();
    }

    public abstract void stop();

    public ExecutorService executorService() {
        return service;
    }
}
