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
package org.geysermc.databaseutils;

import java.util.concurrent.ExecutorService;
import org.geysermc.databaseutils.codec.TypeCodecRegistry;

public record DatabaseContext(
        String url,
        String username,
        String password,
        String poolName,
        int connectionPoolSize,
        DatabaseWithDialectType type,
        ExecutorService service,
        TypeCodecRegistry registry) {

    public DatabaseContext {
        if (poolName == null || poolName.isEmpty())
            throw new IllegalArgumentException("poolName cannot be null or empty");
        if (type == null) throw new IllegalArgumentException("A database type has to be provided");
    }

    public DatabaseContext(
            DatabaseConfig config,
            String poolName,
            DatabaseWithDialectType type,
            ExecutorService service,
            TypeCodecRegistry registry) {
        this(
                config.url(),
                config.username(),
                config.password(),
                poolName,
                config.connectionPoolSize(),
                type,
                service,
                registry);
    }
}
