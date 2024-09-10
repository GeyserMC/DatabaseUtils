/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Allows you to change the method name to anything you like, because it will use the value of this annotation.
 * <pre>{@code
 * @Query("deleteByBedrockUsernameAndJavaUsernameAndLinkCodeAndJavaUniqueIdIsNotNull")
 * LinkRequest getAndInvalidateLinkRequest(String, String, String);
 * }</pre>
 */
@Target(ElementType.METHOD)
public @interface Query {
    String value();
}
