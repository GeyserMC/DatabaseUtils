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
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS hello (" +
                        "a " + SqlTypeMappingRegistry.sqlTypeFor(Integer.class, dialect) + ',' +
                        "b " + SqlTypeMappingRegistry.sqlTypeFor(String.class, dialect) + ',' +
                        "c " + SqlTypeMappingRegistry.sqlTypeFor(String.class, dialect) + ',' +
                        "d " + SqlTypeMappingRegistry.sqlTypeFor(UUID.class, dialect) +
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
                                "a " + SqlTypeMappingRegistry.sqlTypeFor(Integer.class, dialect) + ',' +
                                "b " + SqlTypeMappingRegistry.sqlTypeFor(String.class, dialect) + ',' +
                                "c " + SqlTypeMappingRegistry.sqlTypeFor(String.class, dialect) + ',' +
                                "d " + SqlTypeMappingRegistry.sqlTypeFor(UUID.class, dialect) +
                                ")");
                        statement.executeUpdate("CREATE TYPE hello_table AS TABLE OF hello_row");
                    }
                }
            }
        }
    }
}