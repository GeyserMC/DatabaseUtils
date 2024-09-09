package test.advanced;

import com.zaxxer.hikari.HikariDataSource;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.geysermc.databaseutils.codec.TypeCodec;
import org.geysermc.databaseutils.codec.TypeCodecRegistry;
import org.geysermc.databaseutils.sql.SqlDatabase;

public final class AdvancedRepositorySqlImpl implements AdvancedRepository {
    private final SqlDatabase database;

    private final HikariDataSource dataSource;

    private final TypeCodec<UUID> __d;

    public AdvancedRepositorySqlImpl(SqlDatabase database, TypeCodecRegistry registry) {
        this.database = database;
        this.dataSource = database.dataSource();
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
    public void updateCByBAndC(String newValue, String b, String c) {
        try (Connection __connection = this.dataSource.getConnection()) {
            try (PreparedStatement __statement = __connection.prepareStatement("update hello set c=? where b=? and c=?")) {
                __statement.setString(1, newValue);
                __statement.setString(2, b);
                __statement.setString(3, c);
                __statement.executeUpdate();
                return ;
            }
        } catch (SQLException __exception) {
            throw new CompletionException("Unexpected error occurred", __exception);
        }
    }

    @Override
    public CompletableFuture<Boolean> deleteByAAndB(int a, String b) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection __connection = this.dataSource.getConnection()) {
                try (PreparedStatement __statement = __connection.prepareStatement("delete from hello where a=? and b=?")) {
                    __statement.setInt(1, a);
                    __statement.setString(2, b);
                    return __statement.executeUpdate() > 0;
                }
            } catch (SQLException __exception) {
                throw new CompletionException("Unexpected error occurred", __exception);
            }
        } , this.database.executorService());
    }

    @Override
    public Integer deleteByAAndC(int a, String c) {
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
}
