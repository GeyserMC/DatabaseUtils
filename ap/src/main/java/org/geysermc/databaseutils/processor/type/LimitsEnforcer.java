/*
 * Copyright (c) 2024 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/DatabaseUtils
 */
package org.geysermc.databaseutils.processor.type;

import static org.geysermc.databaseutils.processor.type.LimitsEnforcer.DialectTypeLimit.UNLIMITED;
import static org.geysermc.databaseutils.processor.util.CollectionUtils.join;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geysermc.databaseutils.DatabaseType;
import org.geysermc.databaseutils.meta.Length;
import org.geysermc.databaseutils.processor.info.ColumnInfo;
import org.geysermc.databaseutils.processor.info.EntityInfo;
import org.geysermc.databaseutils.processor.info.IndexInfo;
import org.geysermc.databaseutils.processor.util.InvalidRepositoryException;

public class LimitsEnforcer {
    private static final Map<DatabaseType, DialectLimits> LIMITS = new HashMap<>();
    private final EntityInfo entity;

    public LimitsEnforcer(EntityInfo entity) {
        this.entity = entity;
    }

    public void enforce() {
        var suppressed = new HashMap<DatabaseType, List<? extends Throwable>>();

        for (DatabaseType type : DatabaseType.VALUES) {
            var limits = LIMITS.get(type);

            // todo remove
            if (limits == null) {
                continue;
            }

            var failures = enforceSingle(limits);
            if (!failures.isEmpty()) {
                suppressed.put(type, failures);
            }
        }

        if (!suppressed.isEmpty()) {
            var exception = new InvalidRepositoryException(
                    "Entity %s failed validation for the following database types: %s",
                    entity.name(), join(suppressed.keySet()));

            suppressed.forEach((type, failures) -> {
                var baseException =
                        InvalidRepositoryException.createWithoutStackTrace("Failures for database type " + type);
                failures.forEach(baseException::addSuppressed);
                exception.addSuppressed(baseException);
            });

            throw exception;
        }
    }

    private List<InvalidRepositoryException> enforceSingle(DialectLimits limits) {
        var suppressed = new ArrayList<InvalidRepositoryException>();

        if (limits.columnLimit() != -1 && entity.columns().size() > limits.columnLimit()) {
            suppressed.add(InvalidRepositoryException.createWithoutStackTrace(
                    "Expected at most %s columns, got %s", limits.columnLimit(), entity.columns()));
        }

        if (limits.maxIndexLength() != -1 && entity.indexes().size() > limits.maxIndexLength()) {
            suppressed.add(InvalidRepositoryException.createWithoutStackTrace(
                    "Expected at most %s indexes, got %s",
                    limits.maxIndexLength(), entity.indexes().size()));
        }

        if (limits.maxColumnsPerIndex() != -1) {
            for (IndexInfo index : entity.indexes()) {
                if (index.columns().length > limits.maxColumnsPerIndex()) {
                    suppressed.add(InvalidRepositoryException.createWithoutStackTrace(
                            "Expected at most %s columns per index, got %s",
                            limits.maxColumnsPerIndex(), index.columns().length));
                }
            }
        }

        if (limits.maxIdentifierLength() != -1 && entity.name().length() > limits.maxIdentifierLength()) {
            suppressed.add(InvalidRepositoryException.createWithoutStackTrace(
                    "The entity name is longer than allowed. Expected at most %s and got %s",
                    limits.maxIdentifierLength(), entity.name().length()));
        }

        // if there are no type limits, we also can't properly calculate the max row- and max index length
        if (limits.noTypeLimits()) {
            return suppressed;
        }

        if (limits.maxRowLength() != -1) {
            var rowLength = rowLengthFor(entity.columns(), limits);
            if (rowLength > limits.maxRowLength()) {
                suppressed.add(InvalidRepositoryException.createWithoutStackTrace(
                        "The total length of all columns (%s) exceeded the maximum of %s",
                        rowLength, limits.maxRowLength()));
            }
        }

        if (limits.maxIndexLength() != -1) {
            for (IndexInfo index : entity.indexes()) {
                var indexLength = rowLengthFor(entity.columnsFor(index.columns()), limits);

                if (index.type() == IndexInfo.IndexType.PRIMARY) {
                    if (indexLength > limits.maxClusteredIndexLength()) {
                        suppressed.add(InvalidRepositoryException.createWithoutStackTrace(
                                "The total length of all columns of a clustered index (%s) exceeded the maximum of %s",
                                indexLength, limits.maxClusteredIndexLength()));
                    }
                    continue;
                }

                if (indexLength > limits.maxIndexLength()) {
                    suppressed.add(InvalidRepositoryException.createWithoutStackTrace(
                            "The total length of all columns of an index (%s) exceeded the maximum of %s",
                            indexLength, limits.maxIndexLength()));
                }
            }
        }

        return suppressed;
    }

    private int rowLengthFor(List<ColumnInfo> columns, DialectLimits limits) {
        return columns.stream()
                .mapToInt(column -> {
                    var typeLimit = limits.limit(column.typeName());
                    var length = column.annotation(Length.class);
                    // todo allow TypeCodecs to specify the max length, instead of having to specify
                    // it on the fields using the codecs
                    if (length != null) {
                        return typeLimit.validateAndReturnColumnLength(length.max(), column.name());
                    }
                    return typeLimit.validateAndReturnColumnLength(null, column.name());
                })
                .sum();
    }

    static {
        // https://www.mongodb.com/docs/manual/reference/limits/#bson-documents
        // MongoDB doesn't support more than 100 levels of nesting. But we currently don't support nesting anyway
        // https://www.mongodb.com/docs/manual/reference/limits/#mongodb-limit-Length-of-Database-Names
        // https://www.mongodb.com/docs/manual/reference/limits/#namespaces
        // namespace = <database>.<collection>. Namespace length limit = 255, database can be at most 64
        // https://www.mongodb.com/docs/manual/reference/limits/#indexes
        var mongoLimits = new DialectLimits()
                .maxRowLength(-1) // technically it cannot be longer than 16mb
                .maxIndexLength(-1) // unknown, supports at least 124,000
                .columnLimit(-1) // unknown - probably unlimited
                .indexLimit(64)
                .maxColumnsPerIndex(32)
                .maxIdentifierLength(255 - 64) // What remains of namespace - database is max length for collection
                .noTypeLimits(true);
        LIMITS.put(DatabaseType.MONGODB, mongoLimits);

        // https://dev.mysql.com/doc/refman/8.4/en/innodb-limits.html
        // https://dev.mysql.com/doc/refman/8.4/en/glossary.html#glos_index
        // https://dev.mysql.com/doc/refman/8.4/en/innodb-index-types.html
        // https://dev.mysql.com/doc/refman/8.4/en/identifier-length.html
        // https://dev.mysql.com/doc/refman/8.4/en/numeric-type-syntax.html
        // https://dev.mysql.com/doc/refman/8.4/en/integer-types.html
        // https://dev.mysql.com/doc/refman/8.4/en/floating-point-types.html
        // https://dev.mysql.com/doc/refman/8.4/en/storage-requirements.html#data-types-storage-reqs-strings
        // also: the max row size (for all columns combined) is 65535. Have to change some to TEXT or BLOBs
        var mysqlLimits = new DialectLimits()
                .maxRowLength(65535) // MySQL/MariaDB imposed limit
                .maxIndexLength(3072) // InnoDB limit for the default page size, 16KB
                .columnLimit(1017) // InnoDB limit
                .indexLimit(65) // 64 secondary indexes, this doesn't include the primary index
                .maxColumnsPerIndex(16) // MySQL limit
                .maxIdentifierLength(64)
                .limit(Boolean.class, 1) // bool/boolean = synonym for tinyint(1)
                .limit(Byte.class, 1) // bit
                .limit(Short.class, 2) // smallint
                .limit(Character.class, 2) // smallint unsigned
                .limit(Integer.class, 4) // int
                .limit(Long.class, 8) // bigint
                .limit(Float.class, 8) // real - alias for double
                .limit(Double.class, 8) // double - double precision
                .limit(String.class, 2, UNLIMITED) // varchar - max in chars! 4 bytes per char (utf8mb4)
                .limit(Byte[].class, 2, UNLIMITED); // 3 bytes for the length prefix
        LIMITS.put(DatabaseType.MYSQL, mysqlLimits);
        // MySQL to MariaDB are almost fully identical when it comes to limits
        // https://mariadb.com/kb/en/innodb-limitations/
        // https://mariadb.com/kb/en/data-types/
        LIMITS.put(DatabaseType.MARIADB, mysqlLimits.copy().maxColumnsPerIndex(32));

        // https://docs.oracle.com/en/database/oracle/oracle-database/23/refrn/logical-database-limits.html
        // https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/CREATE-INDEX.html#GUID-1F89BBC0-825F-4215-AF71-7588E31D8BFE__BGECBJDG
        // https://docs.oracle.com/en/database/oracle/oracle-database/23/refrn/DB_BLOCK_SIZE.html
        // https://docs.oracle.com/en/error-help/db/ora-01450/?r=23ai
        // https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Oracle-Compliance-with-FIPS-127-2.html
        // https://docs.oracle.com/en/database/oracle/oracle-database/23/gmswn/database-gateway-sqlserver-data-type-conversion.html
        // https://docs.oracle.com/en/database/oracle/oracle-database/23/jjdbc/Oracle-extensions.html#GUID-FC9510C6-8FF2-41A7-864A-890B85A316BC
        // https://docs.oracle.com/en/database/oracle/oracle-database/23/sqlrf/Data-Types.html
        // https://docs.oracle.com/en/database/oracle/oracle-database/23/refrn/MAX_STRING_SIZE.html#REFRN10321
        // note that for varchar2 the limit is 4000 bytes (not characters) by default (unless extended), which for utf8
        // can vary between 1000 and 4000 depending on how many bytes every character takes. The base byte count
        // is assumed. It was not found in the documentation
        var oracleLimits = new DialectLimits()
                .maxRowLength(2_000_000)
                .maxIndexLength(6300) // docs mention ~6400 for 8k block size (default), so give it a bit of wiggle room
                .columnLimit(1000) // by default
                .indexLimit(-1)
                .maxColumnsPerIndex(32)
                .maxIdentifierLength(128 - 6) // -6 because of the special types (_table, and _row)
                .limit(Boolean.class, 1) // boolean - unknown length, jdbc seems to indicate 4
                .limit(Byte.class, 3) // tinyint - number(3) / 2 + 1
                .limit(Short.class, 5) // smallint - number(5) / 2 + 1 + 1
                .limit(Character.class, 4) // number(5) / 2 + 1
                .limit(Integer.class, 7) // int - number(10) / 2 + 1 + 1
                .limit(Long.class, 12) // bigint - number(20) / 2 + 1 + 1
                .limit(Float.class, 4) // binary_float
                .limit(Double.class, 8) // binary_double
                .limit(String.class, 2, 4000) // varchar2, see comment above
                .limit(Byte[].class, 2, 2000); // raw, default if not extended
        LIMITS.put(DatabaseType.ORACLE_DATABASE, oracleLimits);

        // https://learn.microsoft.com/en-us/sql/relational-databases/indexes/clustered-and-nonclustered-indexes-described?view=sql-server-ver16
        // https://en.wikipedia.org/wiki/Database_index#Non-clustered
        // https://learn.microsoft.com/en-us/sql/sql-server/maximum-capacity-specifications-for-sql-server?view=sql-server-ver16
        // https://learn.microsoft.com/en-us/sql/relational-databases/pages-and-extents-architecture-guide?view=sql-server-ver16#large-row-support
        // https://learn.microsoft.com/en-us/sql/t-sql/statements/create-index-transact-sql?view=sql-server-ver16#nonclustered
        // https://learn.microsoft.com/en-us/sql/t-sql/data-types/int-bigint-smallint-and-tinyint-transact-sql?view=sql-server-ver16
        // https://learn.microsoft.com/en-us/sql/t-sql/data-types/float-and-real-transact-sql?view=sql-server-ver16
        // https://learn.microsoft.com/en-us/sql/t-sql/data-types/char-and-varchar-transact-sql?view=sql-server-ver16
        // https://learn.microsoft.com/en-us/sql/t-sql/data-types/binary-and-varbinary-transact-sql?view=sql-server-ver16
        var mssqlLimits = new DialectLimits()
                .maxRowLength(-1) // the soft-limit is 8060 bytes, at which point it uses Large Row Support
                .maxClusteredIndexLength(900)
                .maxIndexLength(1700)
                .columnLimit(1024) // can technically be increased using sparse column sets
                .indexLimit(999)
                .maxColumnsPerIndex(32)
                .maxIdentifierLength(128)
                .limit(Boolean.class, 1) // tinyint
                .limit(Byte.class, 2) // smallint, since tinyint is unsigned
                .limit(Short.class, 2) // smallint
                .limit(Character.class, 4) // int - since char is an unsigned short
                .limit(Integer.class, 4) // int
                .limit(Long.class, 8) // bigint
                .limit(Float.class, 4) // real - float(24)
                .limit(Double.class, 8) // double precision - float(53)
                .limit(String.class, 2, 8000) // varchar, the max is 8000 unless 'max' is used
                .limit(Byte[].class, 2, 8000); // same as varchar
        LIMITS.put(DatabaseType.SQL_SERVER, mssqlLimits);

        // https://www.postgresql.org/docs/16/limits.html
        // https://www.postgresql.org/docs/16/datatype-boolean.html
        // https://www.postgresql.org/docs/16/datatype-numeric.html
        // https://www.postgresql.org/docs/16/datatype-character.html
        // https://www.postgresql.org/docs/16/datatype-binary.html
        // for varchar and bytea: baseByteCount = 1 for small values (126 bytes or less)
        // PostgreSQL uses B-Tree indexes by default, the limits are based of that
        var postgresLimits = new DialectLimits()
                .maxRowLength(-1) // unknown, supports at least 174,000
                .maxIndexLength(-1) // unknown, supports at least 120,000
                .columnLimit(1600)
                .indexLimit(-1)
                .maxColumnsPerIndex(32)
                .maxIdentifierLength(-1) // unknown, at least 500
                .limit(Boolean.class, 1) // boolean
                .limit(Byte.class, 2) // smallint
                .limit(Short.class, 2) // smallint
                .limit(Character.class, 4) // integer - smallint is signed
                .limit(Integer.class, 4) // integer
                .limit(Long.class, 8) // bigint
                .limit(Float.class, 4) // real
                .limit(Double.class, 8) // double precision
                .limit(String.class, 4, 10485760) // varchar - max in chars, not bytes
                .limit(Byte[].class, 4, UNLIMITED); // bytea - no known limit
        LIMITS.put(DatabaseType.POSTGRESQL, postgresLimits);

        // https://h2database.com/html/advanced.html#limits_limitations
        // http://h2database.com/html/datatypes.html
        // no need to specify the other data types, since there is no max row- and no max index length.
        // clob and blob should be used for large strings and binaries respectively
        var h2Limits = new DialectLimits()
                .maxRowLength(-1) // no limit
                .maxIndexLength(-1) // no limit
                .columnLimit(16384)
                .indexLimit(-1) // no limit
                .maxColumnsPerIndex(-1) // unknown, at least 45 - probably unlimited
                .maxIdentifierLength(256)
                .noTypeLimits(true); // technically there is a limit of 1,000,000,000 for varchar and varbinary
        LIMITS.put(DatabaseType.H2, h2Limits);

        // https://www.sqlite.org/limits.html
        // https://www.sqlite.org/lang_createindex.html
        // https://www.sqlite.org/quirks.html
        // https://www.sqlite.org/datatype3.html
        var sqliteLimits = new DialectLimits()
                .maxRowLength(-1) // technically, since one row = one blob, the limit is 1,000,000,000
                .maxIndexLength(-1) // unknown, probably the same as row length limit
                .columnLimit(2000)
                .indexLimit(-1) // no limit
                .maxColumnsPerIndex(2000)
                .maxIdentifierLength(-1) // unknown, at least 500 - probably unlimited
                .noTypeLimits(true); // technically there is a limit of 1,000,000,000 for strings and blobs
        LIMITS.put(DatabaseType.SQLITE, sqliteLimits);
    }

    private static class DialectLimits {
        private final Map<String, DialectTypeLimit> typeLimits = new HashMap<>();
        private int maxRowLength;
        private int maxClusteredIndexLength = Integer.MIN_VALUE;
        private int maxIndexLength;
        private int columnLimit;
        private int indexLimit;
        private int maxColumnsPerIndex;
        private int maxIdentifierLength;
        private boolean noTypeLimits = false;

        public DialectTypeLimit limit(Class<?> clazz) {
            return limit(clazz.getCanonicalName());
        }

        public DialectTypeLimit limit(String clazz) {
            var limit = typeLimits.get(clazz);
            if (limit == null) {
                return limit(Byte[].class);
            }
            return limit;
        }

        public DialectTypeLimit limit(CharSequence clazz) {
            return limit(clazz.toString());
        }

        public DialectLimits limit(Class<?> clazz, int byteCount) {
            typeLimits.put(clazz.getCanonicalName(), new DialectTypeLimit(byteCount));
            return this;
        }

        public DialectLimits limit(Class<?> clazz, int baseByteCount, int maxVaryingLength) {
            typeLimits.put(clazz.getCanonicalName(), new DialectTypeLimit(baseByteCount, maxVaryingLength));
            return this;
        }

        public DialectLimits maxRowLength(int rowLimit) {
            this.maxRowLength = rowLimit;
            return this;
        }

        /**
         * Returns the max length of a single row, or -1 if there is no limit.
         */
        public int maxRowLength() {
            return maxRowLength;
        }

        public int maxClusteredIndexLength() {
            if (maxClusteredIndexLength == Integer.MIN_VALUE) {
                return maxIndexLength;
            }
            return maxClusteredIndexLength;
        }

        public DialectLimits maxClusteredIndexLength(int maxClusteredIndexLength) {
            this.maxClusteredIndexLength = maxClusteredIndexLength;
            return this;
        }

        /**
         * Returns the max length of a single index, or -1 if there is no limit.
         */
        public int maxIndexLength() {
            return maxIndexLength;
        }

        public DialectLimits maxIndexLength(int maxIndexLength) {
            this.maxIndexLength = maxIndexLength;
            return this;
        }

        /**
         * Returns the max number of columns in a single table, or -1 if there is no limit.
         */
        public int columnLimit() {
            return columnLimit;
        }

        public DialectLimits columnLimit(int columnLimit) {
            this.columnLimit = columnLimit;
            return this;
        }

        /**
         * Returns the max amount of indexes in a table, or -1 if there is no limit.
         */
        public int indexLimit() {
            return indexLimit;
        }

        public DialectLimits indexLimit(int indexLimit) {
            this.indexLimit = indexLimit;
            return this;
        }

        /**
         * Returns the max amount of columns allowed per index, or -1 if there is no limit.
         */
        public int maxColumnsPerIndex() {
            return maxColumnsPerIndex;
        }

        public DialectLimits maxColumnsPerIndex(int maxColumnsPerIndex) {
            this.maxColumnsPerIndex = maxColumnsPerIndex;
            return this;
        }

        /**
         * Returns the max length of an identifier, or -1 if there is no limit
         */
        public int maxIdentifierLength() {
            return maxIdentifierLength;
        }

        public DialectLimits maxIdentifierLength(int maxIdentifierLength) {
            this.maxIdentifierLength = maxIdentifierLength;
            return this;
        }

        public boolean noTypeLimits() {
            return noTypeLimits;
        }

        public DialectLimits noTypeLimits(boolean noTypeLimits) {
            this.noTypeLimits = noTypeLimits;
            return this;
        }

        public DialectLimits copy() {
            var copy = new DialectLimits();
            copy.typeLimits.putAll(typeLimits);
            copy.maxRowLength = maxRowLength;
            copy.maxClusteredIndexLength = maxClusteredIndexLength;
            copy.maxIndexLength = maxIndexLength;
            copy.columnLimit = columnLimit;
            copy.indexLimit = indexLimit;
            copy.maxColumnsPerIndex = maxColumnsPerIndex;
            copy.maxIdentifierLength = maxIdentifierLength;
            copy.noTypeLimits = noTypeLimits;
            return copy;
        }
    }

    record DialectTypeLimit(int baseByteCount, int maxVaryingLength) {
        static final int UNSET = -2;
        static final int UNLIMITED = -1;

        private DialectTypeLimit(int base) {
            this(base, UNSET);
        }

        public boolean varying() {
            return maxVaryingLength == UNLIMITED || maxVaryingLength > 0;
        }

        @Override
        public int maxVaryingLength() {
            return maxVaryingLength == UNLIMITED ? Integer.MAX_VALUE : maxVaryingLength;
        }

        public int validateAndReturnColumnLength(Integer selfDefinedLength, CharSequence columnName) {
            if (selfDefinedLength == null && varying()) {
                throw new InvalidRepositoryException(
                        "Expected %s to have a Length annotation specifying the max length", columnName);
            }
            if (!varying()) {
                return baseByteCount;
            }

            if (selfDefinedLength <= maxVaryingLength()) {
                return baseByteCount + selfDefinedLength;
            }
            throw new InvalidRepositoryException(
                    "Expected %s to have a max length of at most %s bytes, got %s",
                    columnName, maxVaryingLength(), selfDefinedLength);
        }
    }
}
