/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.geysermc.databaseutils.Database;
import org.geysermc.databaseutils.DatabaseContext;
import org.geysermc.databaseutils.util.ClassUtils;

public final class SqlDatabase extends Database {
    private SqlDialect dialect;
    private HikariDataSource dataSource;

    @Override
    public void start(DatabaseContext context, Class<?> databaseImpl) {
        super.start(context, databaseImpl);
        this.dialect = context.type().dialect();

        if (dialect == null) {
            throw new IllegalStateException("The selected type '" + context.type() + "' is not a sql type");
        }

        // This also loads the driver
        if (!ClassUtils.isClassPresent(dialect.driverName())) {
            throw new IllegalStateException("The driver for the selected dialect '" + dialect + "' is not present!");
        }

        var config = context.config();

        var hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.url());
        hikariConfig.setUsername(config.username());
        hikariConfig.setPassword(config.password());
        hikariConfig.setPoolName(context.poolName());
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
