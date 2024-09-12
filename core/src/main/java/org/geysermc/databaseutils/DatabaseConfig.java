/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils;

public record DatabaseConfig(String url, String username, String password, int connectionPoolSize) {
    public DatabaseConfig {
        if (username != null && (username.isEmpty() || "null".equals(username))) {
            username = null;
        }
        if (password != null && (password.isEmpty() || "null".equals(password))) {
            password = null;
        }

        if (url == null || url.isEmpty()) throw new IllegalArgumentException("url cannot be null or empty");
        if (connectionPoolSize <= 0 && connectionPoolSize != -1) {
            throw new IllegalArgumentException("connectionPoolSize has to be at least 1, or -1 to not define one");
        }
    }
}
