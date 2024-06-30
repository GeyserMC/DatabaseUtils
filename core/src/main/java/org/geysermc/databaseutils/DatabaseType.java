/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils;

public enum DatabaseType {
    SQL("Sql"),
    MONGODB("Mongo");

    private final String upperCamelCaseName;

    DatabaseType(String upperCamelCaseName) {
        this.upperCamelCaseName = upperCamelCaseName;
    }

    public String upperCamelCaseName() {
        return upperCamelCaseName;
    }
}
