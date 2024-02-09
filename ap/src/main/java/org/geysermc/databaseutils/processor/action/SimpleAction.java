package org.geysermc.databaseutils.processor.action;

import com.squareup.javapoet.MethodSpec;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.query.QueryInfo;
import org.geysermc.databaseutils.processor.query.section.QuerySection;
import org.geysermc.databaseutils.processor.type.RepositoryGenerator;

abstract class SimpleAction extends Action {
    protected SimpleAction(String actionType) {
        super(actionType, '^' + actionType + '$');
    }

    @Override
    public List<QuerySection> querySectionsFor(
            String fullName, ExecutableElement element, TypeElement returnType, EntityInfo info, Types typeUtils) {
        //todo
        return Collections.emptyList();
    }

    @Override
    public void addTo(RepositoryGenerator generator, QueryInfo queryInfo, MethodSpec.Builder spec, boolean async) {
        generator.addSimple(actionType(), queryInfo, spec, async);
    }
}
