/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.sql;

public enum SqlDialect {
    H2("org.h2.Driver"),
    SQL_SERVER("com.microsoft.sqlserver.jdbc.SQLServerDriver"),
    MYSQL("org.mariadb.jdbc.Driver"),
    MARIADB("org.mariadb.jdbc.Driver"),
    ORACLE_DATABASE("oracle.jdbc.driver.OracleDriver"),
    POSTGRESQL("org.postgresql.Driver"),
    SQLITE("org.sqlite.JDBC");

    private final String driverName;

    SqlDialect(String driverName) {
        this.driverName = driverName;
    }

    public String driverName() {
        return driverName;
    }
}
