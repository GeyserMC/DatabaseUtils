/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.type.sql;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Locale;
import org.geysermc.databaseutils.DatabaseCategory;
import org.geysermc.databaseutils.processor.info.ColumnInfo;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.type.DatabaseGenerator;
import org.geysermc.databaseutils.sql.SqlDatabase;
import org.geysermc.databaseutils.sql.SqlDialect;
import org.geysermc.databaseutils.sql.SqlTypeMappingRegistry;

public class SqlDatabaseGenerator extends DatabaseGenerator {
    public SqlDatabaseGenerator() {
        super(DatabaseCategory.SQL);
    }

    @Override
    public Class<?> databaseClass() {
        return SqlDatabase.class;
    }

    @Override
    protected void addEntities(Collection<EntityInfo> entities, MethodSpec.Builder method) {
        method.addException(SQLException.class);
        method.addStatement("$T dialect = database.dialect()", SqlDialect.class);

        method.beginControlFlow("try ($T connection = database.dataSource().getConnection())", Connection.class);
        method.beginControlFlow("try ($T statement = connection.createStatement())", Statement.class);

        for (EntityInfo entity : entities) {
            method.addStatement("statement.executeUpdate($L)", createEntityQuery(entity));
            method.beginControlFlow("if (dialect == $T.$L)", SqlDialect.class, SqlDialect.ORACLE_DATABASE);
            createRowTypes(entity, method);
            method.endControlFlow();
        }

        method.endControlFlow();
        method.endControlFlow();
    }

    private CodeBlock createEntityQuery(EntityInfo entity) {
        // https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Data-Types.html
        // https://docs.oracle.com/en/database/oracle/oracle-database/23/gmswn/database-gateway-sqlserver-data-type-conversion.html

        var builder = CodeBlock.builder();
        // todo indexes are not added atm
        builder.add("\"CREATE TABLE IF NOT EXISTS $L (\" +\n", entity.name());
        createEntityQueryBody(entity, builder);
        builder.add("+\n\")\"");
        return builder.build();
    }

    private void createRowTypes(EntityInfo entity, MethodSpec.Builder method) {
        var rowObject = CodeBlock.builder();
        rowObject.add("$S +\n", "CREATE TYPE " + entity.name() + "_row AS OBJECT(");
        createEntityQueryBody(entity, rowObject);
        rowObject.add("+\n$S", ")");

        method.addStatement("boolean rowExists = false");
        method.beginControlFlow(
                "try (var rs = statement.executeQuery($S))",
                "SELECT COUNT(*) FROM USER_OBJECTS WHERE OBJECT_NAME = '%s' AND STATUS = 'VALID'"
                        .formatted((entity.name() + "_row").toUpperCase(Locale.ROOT)));
        method.beginControlFlow("if (rs.next())");
        method.addStatement("rowExists = rs.getInt(1) > 0");
        method.endControlFlow();
        method.endControlFlow();

        // todo when adding versions take a more proper look into this
        method.beginControlFlow("if (!rowExists)");
        method.addStatement("statement.executeUpdate($L)", rowObject.build());
        method.addStatement(
                "statement.executeUpdate($S)",
                "CREATE TYPE %s_table AS TABLE OF %s_row".formatted(entity.name(), entity.name()));
        method.endControlFlow();
    }

    private void createEntityQueryBody(EntityInfo entity, CodeBlock.Builder builder) {
        boolean first = true;
        for (ColumnInfo column : entity.columns()) {
            if (first) {
                first = false;
            } else {
                builder.add("+ ',' +\n");
            }

            builder.add(
                    "\"$L \" + $T.sqlTypeFor($T.class, dialect) ",
                    column.name(),
                    SqlTypeMappingRegistry.class,
                    column.asType());
        }
    }
}
