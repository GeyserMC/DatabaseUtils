package org.geysermc.databaseutils.processor.action;

import com.squareup.javapoet.MethodSpec;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.query.QueryInfo;
import org.geysermc.databaseutils.processor.type.RepositoryGenerator;
import org.geysermc.databaseutils.processor.util.InvalidRepositoryException;
import org.geysermc.databaseutils.processor.util.TypeUtils;

final class ExistsByAction extends ByAction {
    ExistsByAction() {
        super("existsBy");
    }

    @Override
    protected void validate(ExecutableElement element, TypeElement returnType, EntityInfo info) {
        if (!TypeUtils.isTypeOf(Boolean.class, returnType)) {
            throw new InvalidRepositoryException(
                    "Expected Boolean as return type for %s, got %s", element.getSimpleName(), returnType);
        }
    }

    @Override
    public void addTo(RepositoryGenerator generator, QueryInfo queryInfo, MethodSpec.Builder spec, boolean async) {
        generator.addExistsBy(queryInfo, spec, async);
    }
}
