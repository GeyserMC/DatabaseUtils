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
package org.geysermc.databaseutils.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.concurrent.ExecutorService;
import org.geysermc.databaseutils.Database;
import org.geysermc.databaseutils.DatabaseConfig;

public final class SqlDatabase extends Database {
    private SqlDialect dialect;
    private HikariDataSource dataSource;

    @Override
    public void start(DatabaseConfig config, ExecutorService service) {
        super.start(config, service);
        this.dialect = config.dialect();

        var hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.uri());
        hikariConfig.setUsername(config.username());
        hikariConfig.setPassword(config.password());
        hikariConfig.setPoolName(config.poolName());
        hikariConfig.setMaximumPoolSize(config.connectionPoolSize());

        this.dataSource = new HikariDataSource(hikariConfig);
    }

    @Override
    public void stop() {
        dataSource.close();
    }

    public SqlDialect dialect() {
        return dialect;
    }

    public HikariDataSource dataSource() {
        return dataSource;
    }
}
