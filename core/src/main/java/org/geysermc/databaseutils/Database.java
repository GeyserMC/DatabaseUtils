/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils;

import java.util.concurrent.ExecutorService;

public abstract class Database {
    protected ExecutorService service;
    private boolean started = false;

    public void start(DatabaseContext context, Class<?> databaseImpl) {
        if (started) {
            throw new IllegalStateException("Database instances currently cannot be reused!");
        }
        this.service = context.service();
        this.started = true;
    }

    public abstract void stop();

    public ExecutorService executorService() {
        return service;
    }
}
