/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.util;

import java.io.Serial;

public final class InvalidRepositoryException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 3849728966061779304L;

    public InvalidRepositoryException(String message, Object... arguments) {
        super(String.format(message, arguments));
    }

    private InvalidRepositoryException(boolean disableOwnStackTrace, String message, Object... arguments) {
        super(String.format(message, arguments), null, true, !disableOwnStackTrace);
    }

    public static InvalidRepositoryException createWithoutStackTrace(String message, Object... arguments) {
        return new InvalidRepositoryException(true, message, arguments);
    }
}
