/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.sql;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * The default SQLInput (with SQLInputImpl as impl) is not very flexible.
 * If you expect an int but the database return a BigDecimal (e.g. for the NUMBER type) then it'd fail to cast
 * BigDecimal to Integer.
 */
public final class FlexibleSqlInput {
    private final Object[] attributes;
    private int index = -1;

    public FlexibleSqlInput(Object[] attributes) {
        this.attributes = attributes;
    }

    public String readString() {
        return readString(null);
    }

    public String readString(String defaultValue) {
        Object next = nextAttribute();
        if (next == null) {
            return defaultValue;
        }
        if (next instanceof String string) {
            return string;
        }
        throw new IllegalStateException(
                "Expected a string but got " + next.getClass().getName());
    }

    public Boolean readBoolean() {
        return readBoolean(null);
    }

    public Boolean readBoolean(Boolean defaultValue) {
        Object next = nextAttribute();
        if (next == null) {
            return defaultValue;
        }
        if (next instanceof Boolean booleanValue) {
            return booleanValue;
        }
        throw new IllegalStateException(
                "Expected a boolean but got " + next.getClass().getName());
    }

    public Byte readByte() {
        return readByte(null);
    }

    public Byte readByte(Byte defaultValue) {
        Object next = nextAttribute();
        if (next == null) {
            return defaultValue;
        }
        if (next instanceof Number number) {
            return number.byteValue();
        }
        throw new IllegalStateException(
                "Expected a byte but got " + next.getClass().getName());
    }

    public Short readShort() {
        return readShort(null);
    }

    public Short readShort(Short defaultValue) {
        Object next = nextAttribute();
        if (next == null) {
            return defaultValue;
        }
        if (next instanceof Number number) {
            return number.shortValue();
        }
        throw new IllegalStateException(
                "Expected a short but got " + next.getClass().getName());
    }

    public Integer readInt() {
        return readInt(null);
    }

    public Integer readInt(Integer defaultValue) {
        Object next = nextAttribute();
        if (next == null) {
            return defaultValue;
        }
        if (next instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalStateException(
                "Expected a int but got " + next.getClass().getName());
    }

    public Long readLong() {
        return readLong(null);
    }

    public Long readLong(Long defaultValue) {
        Object next = nextAttribute();
        if (next == null) {
            return defaultValue;
        }
        if (next instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException(
                "Expected a long but got " + next.getClass().getName());
    }

    public Float readFloat() {
        return readFloat(null);
    }

    public Float readFloat(Float defaultValue) {
        Object next = nextAttribute();
        if (next == null) {
            return defaultValue;
        }
        if (next instanceof Number number) {
            return number.floatValue();
        }
        throw new IllegalStateException(
                "Expected a float but got " + next.getClass().getName());
    }

    public Double readDouble() {
        return readDouble(null);
    }

    public Double readDouble(Double defaultValue) {
        Object next = nextAttribute();
        if (next == null) {
            return defaultValue;
        }
        if (next instanceof Number number) {
            return number.doubleValue();
        }
        throw new IllegalStateException(
                "Expected a double but got " + next.getClass().getName());
    }

    public BigDecimal readBigDecimal() {
        return readBigDecimal(null);
    }

    public BigDecimal readBigDecimal(BigDecimal defaultValue) {
        Object next = nextAttribute();
        if (next == null) {
            return defaultValue;
        }
        if (next instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (next instanceof BigInteger bigInteger) {
            return new BigDecimal(bigInteger);
        }
        if (next instanceof Number number) {
            if (next instanceof Float || next instanceof Double) {
                return BigDecimal.valueOf(number.doubleValue());
            }
            return BigDecimal.valueOf(number.longValue());
        }
        throw new IllegalStateException(
                "Expected a BigDecimal but got " + next.getClass().getName());
    }

    public byte[] readBytes() {
        return readBytes(null);
    }

    public byte[] readBytes(byte[] defaultValue) {
        Object next = nextAttribute();
        if (next == null) {
            return defaultValue;
        }
        if (next instanceof byte[] bytes) {
            return bytes;
        }
        throw new IllegalStateException(
                "Expected a byte array but got " + next.getClass().getName());
    }

    private Object nextAttribute() {
        if (++index >= attributes.length) {
            throw new IllegalStateException("Attempted to read more attributes than there are");
        }
        return attributes[index];
    }
}
