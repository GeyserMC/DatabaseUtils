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
package org.geysermc.databaseutils.processor.type;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import org.geysermc.databaseutils.processor.info.ColumnInfo;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.sql.SqlDatabase;
import org.geysermc.databaseutils.sql.SqlDialect;
import org.geysermc.databaseutils.sql.SqlTypeMappingRegistry;

public class SqlDatabaseGenerator extends DatabaseGenerator {
    @Override
    public Class<?> databaseClass() {
        return SqlDatabase.class;
    }

    @Override
    protected void addEntities(Collection<EntityInfo> entities, MethodSpec.Builder method) {
        method.addException(SQLException.class);
        method.addStatement("$T type = database.type()", SqlDialect.class);

        method.beginControlFlow("try ($T connection = database.dataSource().getConnection())", Connection.class);
        method.beginControlFlow("try ($T statement = connection.createStatement())", Statement.class);

        for (EntityInfo entity : entities) {
            method.addStatement(
                    "statement.executeUpdate($L)", addEntityQuery(entity).build());
        }

        method.endControlFlow();
        method.endControlFlow();
    }

    private CodeBlock.Builder addEntityQuery(EntityInfo entity) {
        var builder = CodeBlock.builder();
        builder.add("\"CREATE TABLE IF NOT EXISTS $L (\" +\n", entity.name());

        boolean first = true;
        for (ColumnInfo column : entity.columns()) {
            if (first) {
                first = false;
            } else {
                builder.add("+ ',' +\n");
            }

            builder.add(
                    "\"$L \" + $T.sqlTypeFor($T.class, type) ",
                    column.name(),
                    SqlTypeMappingRegistry.class,
                    ClassName.bestGuess(column.typeName().toString()));
        }
        builder.add("+\n\")\"");
        return builder;
    }
}
