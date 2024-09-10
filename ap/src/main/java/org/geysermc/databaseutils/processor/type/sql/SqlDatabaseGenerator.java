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
        }

        method.endControlFlow();
        method.endControlFlow();
    }

    private CodeBlock createEntityQuery(EntityInfo entity) {
        var builder = CodeBlock.builder();
        // todo indexes are not added atm
        builder.add("\"CREATE TABLE IF NOT EXISTS $L (\" +\n", entity.name());

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
        builder.add("+\n\")\"");
        return builder.build();
    }
}
