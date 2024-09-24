/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.info;

import org.geysermc.databaseutils.meta.Index;

public record IndexInfo(String name, CharSequence[] columns, IndexType type, Index.IndexDirection direction) {
    public IndexInfo(String name, CharSequence[] columns, IndexType type) {
        this(name, columns, type, Index.IndexDirection.ASCENDING);
    }

    public IndexInfo(String name, CharSequence[] columns, boolean unique, Index.IndexDirection direction) {
        this(name, columns, unique ? IndexType.UNIQUE : IndexType.NORMAL, direction);
    }

    public boolean unique() {
        return type == IndexType.UNIQUE || type == IndexType.PRIMARY;
    }

    public enum IndexType {
        PRIMARY,
        UNIQUE,
        NORMAL
    }
}
