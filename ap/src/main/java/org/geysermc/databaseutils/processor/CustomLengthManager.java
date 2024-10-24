/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import javax.annotation.processing.Filer;
import javax.lang.model.type.TypeMirror;
import javax.tools.StandardLocation;
import org.geysermc.databaseutils.meta.Length;
import org.geysermc.databaseutils.processor.util.TypeUtils;

public class CustomLengthManager {
    private final String filePath = "org/geysermc/databaseutils/processor/length-annotated.properties";
    private final TypeUtils typeUtils;
    private final Filer filer;

    private final Map<String, Integer> buildInMaxLengths = new HashMap<>();
    private final Map<String, Integer> readMaxLengths = new HashMap<>();
    private final Map<String, Integer> maxLengths = new HashMap<>();

    public CustomLengthManager(TypeUtils typeUtils, Filer filer) {
        this.typeUtils = typeUtils;
        this.filer = filer;
        buildInMaxLengths.put(UUID.class.getCanonicalName(), 16);
    }

    public void read() {
        try {
            var properties = new Properties();
            try (var stream = getClass().getClassLoader().getResourceAsStream(filePath)) {
                if (stream == null) {
                    return;
                }
                properties.load(stream);
            }
            properties.stringPropertyNames().forEach(key -> {
                readMaxLengths.put(key, Integer.parseInt(properties.getProperty(key)));
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void customLength(TypeMirror mirror, Length length) {
        maxLengths.put(typeUtils.canonicalName(mirror).toString(), length.max());
    }

    public Integer maxLength(TypeMirror mirror) {
        return maxLength(typeUtils.canonicalName(mirror));
    }

    public Integer maxLength(CharSequence name) {
        var nameString = name.toString();
        var maxLength = buildInMaxLengths.get(nameString);
        if (maxLength != null) return maxLength;
        maxLength = maxLengths.get(nameString);
        if (maxLength != null) return maxLength;
        return readMaxLengths.get(nameString);
    }

    public void write() {
        if (maxLengths.isEmpty()) {
            return;
        }

        try {
            var properties = new Properties();
            maxLengths.forEach((k, v) -> properties.setProperty(k, v.toString()));
            var resource = filer.createResource(StandardLocation.CLASS_OUTPUT, "", filePath);
            try (var writer = resource.openWriter()) {
                properties.store(writer, null);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
