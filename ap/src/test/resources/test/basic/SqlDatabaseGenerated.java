package org.geysermc.databaseutils.sql;

import java.lang.Integer;
import java.lang.String;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import org.geysermc.databaseutils.IRepository;
import org.geysermc.databaseutils.codec.TypeCodecRegistry;
import test.basic.BasicRepositorySqlImpl;

class SqlDatabaseGenerated {
    private static final boolean HAS_ASYNC = true;

    private static final List<BiFunction<SqlDatabase, TypeCodecRegistry, IRepository<?>>> REPOSITORIES;

    static {
        REPOSITORIES = new ArrayList<>();
        REPOSITORIES.add(BasicRepositorySqlImpl::new);
    }

    static void createEntities(SqlDatabase database) throws SQLException {
        SqlDialect dialect = database.dialect();
        try (Connection connection = database.dataSource().getConnection()) {
            try (Statement statement = connection.createStatement()) {
                if (dialect == SqlDialect.SQL_SERVER) {
                    statement.executeUpdate("IF OBJECT_ID(N'hello', N'U') IS NULL BEGIN " + "CREATE TABLE hello (" +
                            "a " + SqlTypeMappingRegistry.sqlTypeFor(Integer.class, dialect, -1) + ',' +
                            "b " + SqlTypeMappingRegistry.sqlTypeFor(String.class, dialect, 50) + ',' +
                            "c " + SqlTypeMappingRegistry.sqlTypeFor(String.class, dialect, 10) + ',' +
                            "d " + SqlTypeMappingRegistry.sqlTypeFor(UUID.class, dialect, 16) + ',' +
                            "PRIMARY KEY (a, b)" +
                            ")" + " END");
                } else {
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS hello (" +
                            "a " + SqlTypeMappingRegistry.sqlTypeFor(Integer.class, dialect, -1) + ',' +
                            "b " + SqlTypeMappingRegistry.sqlTypeFor(String.class, dialect, 50) + ',' +
                            "c " + SqlTypeMappingRegistry.sqlTypeFor(String.class, dialect, 10) + ',' +
                            "d " + SqlTypeMappingRegistry.sqlTypeFor(UUID.class, dialect, 16) + ',' +
                            "PRIMARY KEY (a, b)" +
                            ")");
                    if (dialect == SqlDialect.ORACLE_DATABASE) {
                        boolean rowExists = false;
                        try (var rs = statement.executeQuery("SELECT COUNT(*) FROM USER_OBJECTS WHERE OBJECT_NAME = 'HELLO_ROW' AND STATUS = 'VALID'")) {
                            if (rs.next()) {
                                rowExists = rs.getInt(1) > 0;
                            }
                        }
                        if (!rowExists) {
                            statement.executeUpdate("CREATE TYPE hello_row AS OBJECT(" +
                                    "a " + SqlTypeMappingRegistry.sqlTypeFor(Integer.class, dialect, -1) + ',' +
                                    "b " + SqlTypeMappingRegistry.sqlTypeFor(String.class, dialect, 50) + ',' +
                                    "c " + SqlTypeMappingRegistry.sqlTypeFor(String.class, dialect, 10) + ',' +
                                    "d " + SqlTypeMappingRegistry.sqlTypeFor(UUID.class, dialect, 16) +
                                    ")");
                            statement.executeUpdate("CREATE TYPE hello_table AS TABLE OF hello_row");
                        }
                    }
                }
            }
        }
    }
}