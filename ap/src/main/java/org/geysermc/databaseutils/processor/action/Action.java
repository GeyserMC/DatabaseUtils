package org.geysermc.databaseutils.processor.action;

import com.squareup.javapoet.MethodSpec;
import java.util.List;
import java.util.regex.Pattern;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.query.QueryInfo;
import org.geysermc.databaseutils.processor.query.section.QuerySection;
import org.geysermc.databaseutils.processor.type.RepositoryGenerator;

public abstract class Action {
    private final String actionType;
    private final Pattern actionPattern;

    protected Action(String actionType, String actionRegex) {
        this.actionType = actionType;
        this.actionPattern = Pattern.compile(actionRegex);
    }

    public String actionType() {
        return actionType;
    }

    public Pattern actionPattern() {
        return actionPattern;
    }

    public abstract List<QuerySection> querySectionsFor(
            String fullName, ExecutableElement element, TypeElement returnType, EntityInfo info, Types typeUtils);

    public abstract void addTo(RepositoryGenerator generator, QueryInfo queryInfo, MethodSpec.Builder spec, boolean async);
}
