package org.geysermc.databaseutils.processor.action;

import com.squareup.javapoet.MethodSpec;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.query.QueryInfo;
import org.geysermc.databaseutils.processor.type.RepositoryGenerator;
import org.geysermc.databaseutils.processor.util.InvalidRepositoryException;

final class FindByAction extends ByAction {
    FindByAction() {
        super("findBy");
    }

    @Override
    protected void validate(ExecutableElement element, TypeElement returnType, EntityInfo info) {
        if (!returnType.getQualifiedName().contentEquals(info.className())) {
            throw new InvalidRepositoryException(
                    "Expected %s as return type for %s, got %s",
                    info.className(), element.getSimpleName(), returnType);
        }
    }

    @Override
    public void addTo(RepositoryGenerator generator, QueryInfo queryInfo, MethodSpec.Builder spec, boolean async) {
        generator.addFindBy(queryInfo, spec, async);
    }
}
