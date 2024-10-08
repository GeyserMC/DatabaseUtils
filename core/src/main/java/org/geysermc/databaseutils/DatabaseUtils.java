/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.geysermc.databaseutils.codec.TypeCodec;
import org.geysermc.databaseutils.codec.TypeCodecRegistry;

public class DatabaseUtils {
    private final DatabaseContext context;

    private Database database = null;
    private List<IRepository<?>> repositories;

    private DatabaseUtils(DatabaseContext context) {
        this.context = context;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<IRepository<?>> start() {
        var result = new DatabaseLoader().startDatabase(context);
        this.database = result.database();
        this.repositories = result.repositories();
        return result.repositories();
    }

    public void stop() {
        if (database == null) {
            return;
        }
        database.stop();
    }

    public <T extends IRepository<?>> T repositoryFor(Class<T> repository) {
        if (repositories == null) {
            throw new IllegalStateException("Please call start before calling this method!");
        }
        for (IRepository<?> iRepository : repositories) {
            if (repository.isInstance(iRepository)) {
                return repository.cast(iRepository);
            }
        }
        return null;
    }

    public static class Builder {
        private TypeCodecRegistry registry = new TypeCodecRegistry();
        private DatabaseConfig config;
        private String uri;
        private String username;
        private String password;
        private String poolName = "database-utils";
        private int connectionPoolSize = -1;

        private DatabaseType type;

        private Path credentialsFile;
        private boolean useDefaultCredentials = true;

        private ExecutorService executorService;

        private Builder() {}

        public TypeCodecRegistry registry() {
            return registry;
        }

        public Builder registry(TypeCodecRegistry registry) {
            this.registry = registry;
            return this;
        }

        public Builder addCodec(TypeCodec<?> codec) {
            this.registry.addCodec(codec);
            return this;
        }

        public DatabaseConfig config() {
            return config;
        }

        public Builder config(DatabaseConfig config) {
            this.config = config;
            return this;
        }

        public String uri() {
            return uri;
        }

        public Builder uri(String uri) {
            this.uri = uri;
            return this;
        }

        public String username() {
            return username;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public String password() {
            return password;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public String poolName() {
            return poolName;
        }

        public Builder poolName(String poolName) {
            this.poolName = poolName;
            return this;
        }

        public int connectionPoolSize() {
            return connectionPoolSize;
        }

        public Builder connectionPoolSize(int connectionPoolSize) {
            this.connectionPoolSize = connectionPoolSize;
            return this;
        }

        public DatabaseType type() {
            return type;
        }

        public Builder type(DatabaseType type) {
            this.type = type;
            return this;
        }

        public Path credentialsFile() {
            return credentialsFile;
        }

        public Builder credentialsFile(Path credentialsFile) {
            if (credentialsFile != null && Files.exists(credentialsFile) && !Files.isRegularFile(credentialsFile)) {
                throw new IllegalArgumentException("credentialsFile has to be a file, not a directory!");
            }
            this.credentialsFile = credentialsFile;
            return this;
        }

        public boolean useDefaultCredentials() {
            return useDefaultCredentials;
        }

        public Builder useDefaultCredentials(boolean useDefaultCredentials) {
            this.useDefaultCredentials = useDefaultCredentials;
            return this;
        }

        public ExecutorService executorService() {
            return executorService;
        }

        public Builder executorService(ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        public DatabaseUtils build() {
            if (credentialsFile != null && !useDefaultCredentials) {
                throw new IllegalStateException(
                        "Cannot use credentialsFile in combination with not using default credentials");
            }

            var service = executorService;
            if (service == null) {
                service = Executors.newCachedThreadPool();
            }

            var actual = config;
            if (credentialsFile != null) {
                actual = new CredentialsFileHandler().handle(type, credentialsFile);
            } else if (config == null && uri != null) {
                actual = new DatabaseConfig(uri, username, password, connectionPoolSize);
            } else if (useDefaultCredentials) {
                actual = new CredentialsFileHandler().handle(type, null);
            } else {
                throw new IllegalStateException(
                        "At least an uri has to be provided when default credentials are disabled");
            }

            return new DatabaseUtils(new DatabaseContext(actual, poolName, type, service, registry));
        }
    }
}
