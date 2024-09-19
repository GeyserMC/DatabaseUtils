package test.advanced;

import com.zaxxer.hikari.HikariDataSource;
import java.lang.Boolean;
import java.lang.IllegalStateException;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.geysermc.databaseutils.codec.TypeCodec;
import org.geysermc.databaseutils.codec.TypeCodecRegistry;
import org.geysermc.databaseutils.sql.FlexibleSqlInput;
import org.geysermc.databaseutils.sql.SqlDatabase;
import org.geysermc.databaseutils.sql.SqlDialect;

public final class AdvancedRepositorySqlImpl implements AdvancedRepository {
    private final SqlDatabase database;
    private final HikariDataSource dataSource;
    private final SqlDialect dialect;
    private final CommonImpl dialectSpecific;
    private final TypeCodec<UUID> __d;

    public AdvancedRepositorySqlImpl(SqlDatabase database, TypeCodecRegistry registry) {
        this.database = database;
        this.dataSource = database.dataSource();
        this.dialect = database.dialect();
        if (this.dialect == SqlDialect.SQL_SERVER) {
            this.dialectSpecific = new SqlServerImpl();
        } else if (this.dialect == SqlDialect.ORACLE_DATABASE) {
            this.dialectSpecific = new OracleDatabaseImpl();
        } else if (this.dialect == SqlDialect.H2) {
            this.dialectSpecific = new H2Impl();
        } else if (this.dialect == SqlDialect.MYSQL) {
            this.dialectSpecific = new MysqlImpl();
        } else {
            this.dialectSpecific = new CommonImpl();
        }
        this.__d = registry.requireCodecFor(UUID.class);
    }

    @Override
    public CompletableFuture<TestEntity> findByAAndB(int aa, String b) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection __connection = this.dataSource.getConnection()) {
                try (PreparedStatement __statement = __connection.prepareStatement("select * from hello where a=? and b=?")) {
                    __statement.setInt(1, aa);
                    __statement.setString(2, b);
                    try (ResultSet __result = __statement.executeQuery()) {
                        if (!__result.next()) {
                            return null;
                        }
                        Integer _a = __result.getInt("a");
                        String _b = __result.getString("b");
                        String _c = __result.getString("c");
                        UUID _d = this.__d.decode(__result.getBytes("d"));
                        return new TestEntity(_a, _b, _c, _d);
                    }
                }
            } catch (SQLException __exception) {
                throw new CompletionException("Unexpected error occurred", __exception);
            }
        } , this.database.executorService());
    }

    @Override
    public List<String> findTop3BByA(int a) {
        try (Connection __connection = this.dataSource.getConnection()) {
            try (PreparedStatement __statement = __connection.prepareStatement("select b from hello where a=? limit 3")) {
                __statement.setInt(1, a);
                try (ResultSet __result = __statement.executeQuery()) {
                    List<String> __responses = new ArrayList<>();
                    while (__result.next()) {
                        __responses.add(__result.getString("b"));
                    }
                    return __responses;
                }
            }
        } catch (SQLException __exception) {
            throw new CompletionException("Unexpected error occurred", __exception);
        }
    }

    @Override
    public CompletableFuture<Boolean> existsByAOrB(int a, String bb) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection __connection = this.dataSource.getConnection()) {
                try (PreparedStatement __statement = __connection.prepareStatement("select 1 from hello where a=? or b=?")) {
                    __statement.setInt(1, a);
                    __statement.setString(2, bb);
                    try (ResultSet __result = __statement.executeQuery()) {
                        return __result.next();
                    }
                }
            } catch (SQLException __exception) {
                throw new CompletionException("Unexpected error occurred", __exception);
            }
        } , this.database.executorService());
    }

    @Override
    public void updateByBAndC(String b, String oldC, String c) {
        try (Connection __connection = this.dataSource.getConnection()) {
            try (PreparedStatement __statement = __connection.prepareStatement("update hello set c=? where b=? and c=?")) {
                __statement.setString(1, c);
                __statement.setString(2, b);
                __statement.setString(3, oldC);
                __statement.executeUpdate();
                return ;
            }
        } catch (SQLException __exception) {
            throw new CompletionException("Unexpected error occurred", __exception);
        }
    }

    @Override
    public CompletableFuture<Boolean> deleteByAAndBAndC(int a, String b, String c) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection __connection = this.dataSource.getConnection()) {
                try (PreparedStatement __statement = __connection.prepareStatement("delete from hello where a=? and b=? and c=?")) {
                    __statement.setInt(1, a);
                    __statement.setString(2, b);
                    __statement.setString(3, c);
                    return __statement.executeUpdate() > 0;
                }
            } catch (SQLException __exception) {
                throw new CompletionException("Unexpected error occurred", __exception);
            }
        } , this.database.executorService());
    }

    @Override
    public int deleteByAAndC(int a, String c) {
        try (Connection __connection = this.dataSource.getConnection()) {
            try (PreparedStatement __statement = __connection.prepareStatement("delete from hello where a=? and c=?")) {
                __statement.setInt(1, a);
                __statement.setString(2, c);
                return __statement.executeUpdate();
            }
        } catch (SQLException __exception) {
            throw new CompletionException("Unexpected error occurred", __exception);
        }
    }

    @Override
    public TestEntity deleteByAAndB(int a, String b) {
        return this.dialectSpecific.deleteByAAndB(a, b);
    }

    @Override
    public List<TestEntity> deleteByBAndC(String b, String c) {
        return this.dialectSpecific.deleteByBAndC(b, c);
    }

    @Override
    public TestEntity findWithAlternativeName(int a, String b) {
        try (Connection __connection = this.dataSource.getConnection()) {
            try (PreparedStatement __statement = __connection.prepareStatement("select * from hello where a=? and b=? and c is not null")) {
                __statement.setInt(1, a);
                __statement.setString(2, b);
                try (ResultSet __result = __statement.executeQuery()) {
                    if (!__result.next()) {
                        return null;
                    }
                    Integer _a = __result.getInt("a");
                    String _b = __result.getString("b");
                    String _c = __result.getString("c");
                    UUID _d = this.__d.decode(__result.getBytes("d"));
                    return new TestEntity(_a, _b, _c, _d);
                }
            }
        } catch (SQLException __exception) {
            throw new CompletionException("Unexpected error occurred", __exception);
        }
    }

    private class CommonImpl {
        TestEntity deleteByAAndB(int a, String b) {
            try (Connection __connection = AdvancedRepositorySqlImpl.this.dataSource.getConnection()) {
                try (PreparedStatement __statement = __connection.prepareStatement("delete from hello where a=? and b=? returning *")) {
                    __statement.setInt(1, a);
                    __statement.setString(2, b);
                    try (ResultSet __result = __statement.executeQuery()) {
                        if (!__result.next()) {
                            return null;
                        }
                        Integer _a = __result.getInt("a");
                        String _b = __result.getString("b");
                        String _c = __result.getString("c");
                        UUID _d = AdvancedRepositorySqlImpl.this.__d.decode(__result.getBytes("d"));
                        return new TestEntity(_a, _b, _c, _d);
                    }
                }
            } catch (SQLException __exception) {
                throw new CompletionException("Unexpected error occurred", __exception);
            }
        }

        List<TestEntity> deleteByBAndC(String b, String c) {
            try (Connection __connection = AdvancedRepositorySqlImpl.this.dataSource.getConnection()) {
                try (PreparedStatement __statement = __connection.prepareStatement("delete from hello where b=? and c=? returning *")) {
                    __statement.setString(1, b);
                    __statement.setString(2, c);
                    try (ResultSet __result = __statement.executeQuery()) {
                        List<TestEntity> __responses = new ArrayList<>();
                        while (__result.next()) {
                            Integer _a = __result.getInt("a");
                            String _b = __result.getString("b");
                            String _c = __result.getString("c");
                            UUID _d = AdvancedRepositorySqlImpl.this.__d.decode(__result.getBytes("d"));
                            __responses.add(new TestEntity(_a, _b, _c, _d));
                        }
                        return __responses;
                    }
                }
            } catch (SQLException __exception) {
                throw new CompletionException("Unexpected error occurred", __exception);
            }
        }
    }

    private final class SqlServerImpl extends CommonImpl {
        @Override
        TestEntity deleteByAAndB(int a, String b) {
            try (Connection __connection = AdvancedRepositorySqlImpl.this.dataSource.getConnection()) {
                try (PreparedStatement __statement = __connection.prepareStatement("delete from hello output deleted.* where a=? and b=?")) {
                    __statement.setInt(1, a);
                    __statement.setString(2, b);
                    try (ResultSet __result = __statement.executeQuery()) {
                        if (!__result.next()) {
                            return null;
                        }
                        Integer _a = __result.getInt("a");
                        String _b = __result.getString("b");
                        String _c = __result.getString("c");
                        UUID _d = AdvancedRepositorySqlImpl.this.__d.decode(__result.getBytes("d"));
                        return new TestEntity(_a, _b, _c, _d);
                    }
                }
            } catch (SQLException __exception) {
                throw new CompletionException("Unexpected error occurred", __exception);
            }
        }

        @Override
        List<TestEntity> deleteByBAndC(String b, String c) {
            try (Connection __connection = AdvancedRepositorySqlImpl.this.dataSource.getConnection()) {
                try (PreparedStatement __statement = __connection.prepareStatement("delete from hello output deleted.* where b=? and c=?")) {
                    __statement.setString(1, b);
                    __statement.setString(2, c);
                    try (ResultSet __result = __statement.executeQuery()) {
                        List<TestEntity> __responses = new ArrayList<>();
                        while (__result.next()) {
                            Integer _a = __result.getInt("a");
                            String _b = __result.getString("b");
                            String _c = __result.getString("c");
                            UUID _d = AdvancedRepositorySqlImpl.this.__d.decode(__result.getBytes("d"));
                            __responses.add(new TestEntity(_a, _b, _c, _d));
                        }
                        return __responses;
                    }
                }
            } catch (SQLException __exception) {
                throw new CompletionException("Unexpected error occurred", __exception);
            }
        }
    }

    private final class OracleDatabaseImpl extends CommonImpl {
        @Override
        TestEntity deleteByAAndB(int a, String b) {
            try (Connection __connection = AdvancedRepositorySqlImpl.this.dataSource.getConnection()) {
                try (CallableStatement __statement = __connection.prepareCall("BEGIN delete from hello where a=? and b=? returning hello_row(a, b, c, d) into ?; END;")) {
                    __statement.setInt(1, a);
                    __statement.setString(2, b);
                    __statement.registerOutParameter(3, 2002, "HELLO_ROW");
                    __statement.execute();
                    var __result = __statement.getObject(3);
                    if (__result == null) {
                        return null;
                    }
                    var __data = new FlexibleSqlInput(((Struct) __result).getAttributes());
                    Integer _a = __data.readInt();
                    String _b = __data.readString();
                    String _c = __data.readString();
                    UUID _d = AdvancedRepositorySqlImpl.this.__d.decode(__data.readBytes());
                    return new TestEntity(_a, _b, _c, _d);
                }
            } catch (SQLException __exception) {
                throw new CompletionException("Unexpected error occurred", __exception);
            }
        }

        @Override
        List<TestEntity> deleteByBAndC(String b, String c) {
            try (Connection __connection = AdvancedRepositorySqlImpl.this.dataSource.getConnection()) {
                try (CallableStatement __statement = __connection.prepareCall("BEGIN delete from hello where b=? and c=? returning hello_row(a, b, c, d) bulk collect into ?; END;")) {
                    __statement.setString(1, b);
                    __statement.setString(2, c);
                    __statement.registerOutParameter(3, 2003, "HELLO_TABLE");
                    __statement.execute();
                    var __result = (Object[]) __statement.getArray(3).getArray();
                    List<TestEntity> __responses = new ArrayList<>();
                    for (var __item : __result) {
                        var __data = new FlexibleSqlInput(((Struct) __item).getAttributes());
                        Integer _a = __data.readInt();
                        String _b = __data.readString();
                        String _c = __data.readString();
                        UUID _d = AdvancedRepositorySqlImpl.this.__d.decode(__data.readBytes());
                        __responses.add(new TestEntity(_a, _b, _c, _d));
                    }
                    return __responses;
                }
            } catch (SQLException __exception) {
                throw new CompletionException("Unexpected error occurred", __exception);
            }
        }
    }

    private final class H2Impl extends CommonImpl {
        @Override
        TestEntity deleteByAAndB(int a, String b) {
            throw new IllegalStateException("This behaviour is not yet implemented!");
        }

        @Override
        List<TestEntity> deleteByBAndC(String b, String c) {
            throw new IllegalStateException("This behaviour is not yet implemented!");
        }
    }

    private final class MysqlImpl extends CommonImpl {
        @Override
        TestEntity deleteByAAndB(int a, String b) {
            throw new IllegalStateException("This behaviour is not yet implemented!");
        }

        @Override
        List<TestEntity> deleteByBAndC(String b, String c) {
            throw new IllegalStateException("This behaviour is not yet implemented!");
        }
    }
}
