package org.geysermc.databaseutils.processor.query.section;

import java.util.Map;
import org.geysermc.databaseutils.processor.query.section.selector.AndSelector;
import org.geysermc.databaseutils.processor.query.section.selector.OrSelector;

public final class QuerySectionRegistry {
    private static final Map<String, QuerySection> SELECTORS =
            Map.of("And", AndSelector.INSTANCE, "Or", OrSelector.INSTANCE);

    private QuerySectionRegistry() {}

    public static QuerySection selectorFor(String name) {
        return SELECTORS.get(name);
    }
}
