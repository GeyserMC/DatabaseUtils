package test.advanced;

import com.zaxxer.hikari.HikariDataSource;
import java.lang.Boolean;
import java.lang.IllegalStateException;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.geysermc.databaseutils.codec.TypeCodec;
import org.geysermc.databaseutils.codec.TypeCodecRegistry;
import org.geysermc.databaseutils.sql.SqlDatabase;
import org.geysermc.databaseutils.sql.SqlDialect;

public final class AdvancedRepositorySqlImpl implements AdvancedRepository {
    private final SqlDatabase database;
    private final HikariDataSource dataSource;
    private final SqlDialect dialect;
    private final TypeCodec<UUID> __d;

    public AdvancedRepositorySqlImpl(SqlDatabase database, TypeCodecRegistry registry) {
        this.database = database;
        this.dataSource = database.dataSource();
        this.dialect = database.dialect();
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
        String __sql;
        if (this.dialect == SqlDialect.POSTGRESQL || this.dialect == SqlDialect.ORACLE_DATABASE || this.dialect == SqlDialect.SQLITE || this.dialect == SqlDialect.MARIADB) {
            __sql = "delete from hello where a=? and b=? returning *";
        } else if (this.dialect == SqlDialect.SQL_SERVER) {
            __sql = "delete from hello output deleted.* where a=? and b=?";
        } else if (this.dialect == SqlDialect.H2 || this.dialect == SqlDialect.MYSQL) {
            throw new IllegalStateException("This behaviour is not yet implemented!");
        } else {
            throw new IllegalStateException("Unexpected dialect " + dialect);
        }
        try (final Connection __connection = this.dataSource.getConnection()) {
            try (final PreparedStatement __statement = __connection.prepareStatement(__sql)) {
                __statement.setInt(1, a);
                __statement.setString(2, b);
                try (final ResultSet __result = __statement.executeQuery();) {
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

    @Override
    public List<TestEntity> deleteByBAndC(String b, String c) {
        String __sql;
        if (this.dialect == SqlDialect.POSTGRESQL || this.dialect == SqlDialect.ORACLE_DATABASE || this.dialect == SqlDialect.SQLITE || this.dialect == SqlDialect.MARIADB) {
            __sql = "delete from hello where b=? and c=? returning *";
        } else if (this.dialect == SqlDialect.SQL_SERVER) {
            __sql = "delete from hello output deleted.* where b=? and c=?";
        } else if (this.dialect == SqlDialect.H2 || this.dialect == SqlDialect.MYSQL) {
            throw new IllegalStateException("This behaviour is not yet implemented!");
        } else {
            throw new IllegalStateException("Unexpected dialect " + dialect);
        }
        try (Connection __connection = this.dataSource.getConnection()) {
            try (PreparedStatement __statement = __connection.prepareStatement(__sql)) {
                __statement.setString(1, b);
                __statement.setString(2, c);
                try (ResultSet __result = __statement.executeQuery()) {
                    List<TestEntity> __responses = new ArrayList<>();
                    while (__result.next()) {
                        Integer _a = __result.getInt("a");
                        String _b = __result.getString("b");
                        String _c = __result.getString("c");
                        UUID _d = this.__d.decode(__result.getBytes("d"));
                        __responses.add(new TestEntity(_a, _b, _c, _d));
                    }
                    return __responses;
                }
            }
        } catch (SQLException __exception) {
            throw new CompletionException("Unexpected error occurred", __exception);
        }
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
}
