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
package org.geysermc.databaseutils.processor.action;

import com.squareup.javapoet.MethodSpec;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Types;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.query.QueryInfo;
import org.geysermc.databaseutils.processor.query.section.QuerySection;
import org.geysermc.databaseutils.processor.query.section.QuerySectionsReader;
import org.geysermc.databaseutils.processor.type.RepositoryGenerator;

abstract class ByAction extends Action {
    protected ByAction(String actionType) {
        super(actionType, '^' + actionType + ".*");
    }

    protected abstract void validate(ExecutableElement element, TypeElement returnType, EntityInfo info);

    protected abstract void addToSingle(
            RepositoryGenerator generator, QueryInfo queryInfo, MethodSpec.Builder spec, boolean async);

    @Override
    public void addTo(
            List<RepositoryGenerator> generators,
            String fullName,
            ExecutableElement element,
            TypeElement returnType,
            EntityInfo info,
            Types typeUtils,
            boolean async) {
        var sections = querySectionsFor(fullName, element, returnType, info, typeUtils);
        var parameterNames = element.getParameters().stream()
                .map(VariableElement::getSimpleName)
                .toList();
        var queryInfo = new QueryInfo(info.name(), info.className(), info.columns(), sections, parameterNames);
        for (RepositoryGenerator generator : generators) {
            addToSingle(generator, queryInfo, MethodSpec.overriding(element), async);
        }
    }

    protected List<QuerySection> querySectionsFor(
            String fullName, ExecutableElement element, TypeElement returnType, EntityInfo info, Types typeUtils) {
        validate(element, returnType, info);
        return new QuerySectionsReader(
                        actionType(), fullName.substring(actionType().length()), element, info, typeUtils)
                .readBySections();
    }
}
