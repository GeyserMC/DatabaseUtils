/*
 * Copyright (c) 2024 GeyserMC <https://geysermc.org>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class DatabaseUtils {
    private final DatabaseConfig config;
    private final ExecutorService executorService;

    private Database database = null;
    private List<IRepository<?>> repositories;

    private DatabaseUtils(DatabaseConfig config, ExecutorService executorService) {
        this.config = config;
        this.executorService = executorService;
    }

    public List<IRepository<?>> start() {
        new DatabaseLoader();
        return null;
    }

    public void stop() {
        if (database == null) {
            return;
        }
        database.stop();
    }

    public static class Builder {
        private DatabaseConfig config;
        private String uri;
        private String username;
        private String password;
        private String poolName;
        private int connectionPoolSize;
        private ExecutorService executorService;

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

        public ExecutorService executorService() {
            return executorService;
        }

        public Builder executorService(ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        public DatabaseUtils build() {
            return new DatabaseUtils(
                    config != null ? config : new DatabaseConfig(uri, username, password, poolName, connectionPoolSize),
                    executorService);
        }
    }
}
