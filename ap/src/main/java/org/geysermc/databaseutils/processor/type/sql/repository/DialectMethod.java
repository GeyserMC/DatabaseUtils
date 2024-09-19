/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.type.sql.repository;

import static org.geysermc.databaseutils.processor.util.CollectionUtils.map;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DialectMethod {
    private final MethodSpec.Builder builder;
    private final Map<Identifier, SpecAction> actionsById = new HashMap<>();
    private final List<SpecAction> actionOrder = new ArrayList<>();

    private final String className;

    public DialectMethod(MethodSpec.Builder builder, String className) {
        this.builder = builder;
        this.className = className;
    }

    public DialectMethod(MethodSpec.Builder builder) {
        this(builder, null);
    }

    public enum Identifier {
        THROW_1,
        PREPARE_STATEMENT
    }

    public boolean shouldAdd() {
        return className == null;
    }

    public DialectMethod setThrow(Class<? extends Throwable> exception, String message) {
        return addStatement(Identifier.THROW_1, "throw new $T($S)", exception, message);
    }

    public DialectMethod addStatement(String format, Object... args) {
        return addStatement(null, format, args);
    }

    public DialectMethod addStatement(CodeBlock block) {
        // todo remove CodeBlock since replaceThis isn't run for them
        return addStatement(null, block);
    }

    public DialectMethod addStatement(Identifier identifier, String format, Object... args) {
        return addStatement(identifier, CodeBlock.of(replaceThis(format), map(args, arg -> {
            if (arg instanceof String string) {
                return replaceThis(string);
            }
            return arg;
        })));
    }

    public DialectMethod addStatement(Identifier identifier, CodeBlock block) {
        return addCommon(identifier, new AddStatement(block));
    }

    public DialectMethod updateStatement(Identifier identifier, String format, Object... args) {
        return replaceCommon(identifier, new AddStatement(CodeBlock.of(replaceThis(format), args)));
    }

    public DialectMethod beginControlFlow(String controlFlow, Object... args) {
        return beginControlFlow(null, controlFlow, args);
    }

    public DialectMethod beginControlFlow(Identifier identifier, String controlFlow, Object... args) {
        return addCommon(identifier, new BeginControlFlow(replaceThis(controlFlow), args));
    }

    public DialectMethod replaceBeginControlFlow(Identifier identifier, String controlFlow, Object... args) {
        return replaceCommon(identifier, new BeginControlFlow(replaceThis(controlFlow), args));
    }

    public DialectMethod nextControlFlow(String controlFlow, Object... args) {
        return nextControlFlow(null, controlFlow, args);
    }

    public DialectMethod nextControlFlow(Identifier identifier, String controlFlow, Object... args) {
        return addCommon(identifier, new NextControlFlow(replaceThis(controlFlow), args));
    }

    public DialectMethod endControlFlow() {
        return addCommon(null, new EndControlFlowSimple());
    }

    public DialectMethod endControlFlow(String controlFlow, Object... args) {
        return addCommon(null, new EndControlFlow(replaceThis(controlFlow), args));
    }

    private DialectMethod addCommon(Identifier identifier, SpecAction action) {
        var oldValue = actionsById.putIfAbsent(identifier, action);
        if (oldValue != null && identifier != null) {
            throw new IllegalArgumentException("Duplicate statement with identifier: " + identifier);
        }
        actionOrder.add(action);
        return this;
    }

    private DialectMethod replaceCommon(Identifier identifier, SpecAction action) {
        var oldValue = actionsById.put(identifier, action);
        if (oldValue == null) {
            throw new IllegalArgumentException("Expected to override statement with identifier: " + identifier);
        }
        actionOrder.set(actionOrder.indexOf(oldValue), action);
        return this;
    }

    public MethodSpec.Builder builder() {
        return builder;
    }

    public MethodSpec build() {
        for (SpecAction action : actionOrder) {
            if (action instanceof AddStatement statement) {
                builder.addStatement(statement.block);
            } else if (action instanceof BeginControlFlow flow) {
                builder.beginControlFlow(flow.controlFlow, flow.args);
            } else if (action instanceof NextControlFlow flow) {
                builder.nextControlFlow(flow.controlFlow, flow.args);
            } else if (action instanceof EndControlFlowSimple) {
                builder.endControlFlow();
            } else if (action instanceof EndControlFlow flow) {
                builder.endControlFlow(flow.controlFlow, flow.args);
            }
        }
        return builder.build();
    }

    private String replaceThis(String flow) {
        if (className == null) {
            return flow;
        }
        return flow.replace("this.", "%s.this.".formatted(className));
    }

    private interface SpecAction {}

    private record AddStatement(CodeBlock block) implements SpecAction {}

    private record BeginControlFlow(String controlFlow, Object... args) implements SpecAction {}

    private record NextControlFlow(String controlFlow, Object... args) implements SpecAction {}

    private record EndControlFlowSimple() implements SpecAction {}

    private record EndControlFlow(String controlFlow, Object... args) implements SpecAction {}
}
