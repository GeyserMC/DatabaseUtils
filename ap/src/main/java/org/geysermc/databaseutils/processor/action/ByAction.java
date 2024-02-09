package org.geysermc.databaseutils.processor.action;

import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.query.section.QuerySection;
import org.geysermc.databaseutils.processor.query.section.QuerySectionsReader;

abstract class ByAction extends Action {
    protected ByAction(String actionType) {
        super(actionType, '^' + actionType + ".*");
    }

    protected abstract void validate(ExecutableElement element, TypeElement returnType, EntityInfo info);

    @Override
    public List<QuerySection> querySectionsFor(String fullName, ExecutableElement element, TypeElement returnType, EntityInfo info, Types typeUtils) {
        validate(element, returnType, info);
        return new QuerySectionsReader(actionType(), fullName.substring(actionType().length()), element, info, typeUtils)
                .readBySections();
    }
}
