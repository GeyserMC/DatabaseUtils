/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.action;

import com.squareup.javapoet.MethodSpec;
import org.geysermc.databaseutils.processor.query.QueryContext;
import org.geysermc.databaseutils.processor.type.RepositoryGenerator;

final class InsertAction extends Action {
    InsertAction() {
        // todo add allowReturnAnySelfOrColumn support
        super("insert", true, false, false, false);
    }

    @Override
    protected void addToSingle(RepositoryGenerator generator, QueryContext context, MethodSpec.Builder spec) {
        generator.addInsert(context, spec);
    }
}
