/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils;

import static org.geysermc.databaseutils.util.StringUtils.nullToEmpty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Properties;

final class CredentialsFileHandler {
    public DatabaseConfig handle(DatabaseType dialect, Path credentialsFile) {
        DatabaseConfig config = defaultValuesFor(dialect);
        if (credentialsFile != null) {
            if (Files.exists(credentialsFile)) {
                config = readConfig(credentialsFile);
            } else {
                createConfig(config, credentialsFile);
            }
        }
        return config;
    }

    private DatabaseConfig readConfig(Path credentialsFile) {
        var properties = new Properties();
        try (var reader = Files.newBufferedReader(credentialsFile)) {
            properties.load(reader);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load credentials!", exception);
        }
        return new DatabaseConfig(
                properties.getProperty("url"),
                properties.getProperty("username"),
                properties.getProperty("password"),
                Integer.parseInt(properties.getProperty("connectionPoolSize")));
    }

    private void createConfig(DatabaseConfig defaults, Path toStore) {
        // not using properties here so that the values aren't escaped and there isn't a timestamp comment
        var lines = new ArrayList<String>();
        lines.add("# Database configuration");
        lines.add("url=" + defaults.url());
        lines.add("username=" + nullToEmpty(defaults.username()));
        lines.add("password=" + nullToEmpty(defaults.password()));
        lines.add("connectionPoolSize=" + defaults.connectionPoolSize());
        try {
            Files.write(toStore, lines);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save credential defaults!", exception);
        }
    }

    private DatabaseConfig defaultValuesFor(DatabaseType type) {
        return switch (type) {
            case H2 -> configFor("jdbc:h2:./database", "sa");
            case SQL_SERVER -> configFor("jdbc:sqlserver://localhost;encrypt=true;integratedSecurity=true;");
            case MYSQL -> configFor("jdbc:mysql://localhost/database");
            case ORACLE_DATABASE -> configFor("jdbc:oracle:thin:@//localhost/service");
            case POSTGRESQL -> configFor("jdbc:postgresql://localhost/database");
            case SQLITE -> configFor("jdbc:sqlite:./database");
            case MONGODB -> configFor("mongodb://localhost:27017/database");
        };
    }

    private DatabaseConfig configFor(String url) {
        return configFor(url, null);
    }

    private DatabaseConfig configFor(String url, String username) {
        // default pool size is 5
        return new DatabaseConfig(url, username, null, 5);
    }
}
