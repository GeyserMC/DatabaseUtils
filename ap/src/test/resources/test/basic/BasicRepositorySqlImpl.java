package test.basic;

import com.zaxxer.hikari.HikariDataSource;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.lang.Void;
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

public final class BasicRepositorySqlImpl implements BasicRepository {
    private final SqlDatabase database;

    private final HikariDataSource dataSource;

    private final TypeCodec<UUID> __d;

    public BasicRepositorySqlImpl(SqlDatabase database, TypeCodecRegistry registry) {
        this.database = database;
        this.dataSource = database.dataSource();
        this.__d = registry.requireCodecFor(UUID.class);
    }

    @Override
    public CompletableFuture<TestEntity> findByAAndB(int aa, String b) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("select * from hello where a=? and b=?")) {
                    statement.setInt(1, aa);
                    statement.setString(2, b);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            return null;
                        }
                        Integer _a = result.getInt("a");
                        String _b = result.getString("b");
                        String _c = result.getString("c");
                        UUID _d = this.__d.decode(result.getBytes("d"));
                        return new TestEntity(_a, _b, _c, _d);
                    }
                }
            } catch (SQLException exception) {
                throw new CompletionException("Unexpected error occurred", exception);
            }
        } , this.database.executorService());
    }

    @Override
    public CompletableFuture<Boolean> existsByAOrB(int a, String bb) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("select 1 from hello where a=? or b=?")) {
                    statement.setInt(1, a);
                    statement.setString(2, bb);
                    try (ResultSet result = statement.executeQuery()) {
                        return result.next();
                    }
                }
            } catch (SQLException exception) {
                throw new CompletionException("Unexpected error occurred", exception);
            }
        } , this.database.executorService());
    }

    @Override
    public TestEntity update(TestEntity entity) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("update hello set c=?,d=? where a=? and b=?")) {
                statement.setString(1, entity.c());
                statement.setBytes(2, this.__d.encode(entity.d()));
                statement.setInt(3, entity.a());
                statement.setString(4, entity.b());
                statement.executeUpdate();
                return entity;
            }
        } catch (SQLException exception) {
            throw new CompletionException("Unexpected error occurred", exception);
        }
    }

    @Override
    public CompletableFuture<Void> insert(TestEntity entity) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("insert into hello (a,b,c,d) values (?,?,?,?)")) {
                    statement.setInt(1, entity.a());
                    statement.setString(2, entity.b());
                    statement.setString(3, entity.c());
                    statement.setBytes(4, this.__d.encode(entity.d()));
                    statement.executeUpdate();
                    return null;
                }
            } catch (SQLException exception) {
                throw new CompletionException("Unexpected error occurred", exception);
            }
        } , this.database.executorService());
    }

    @Override
    public CompletableFuture<Void> delete(TestEntity entity) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("delete from hello where a=? and b=?")) {
                    statement.setInt(1, entity.a());
                    statement.setString(2, entity.b());
                    statement.executeUpdate();
                    return null;
                }
            } catch (SQLException exception) {
                throw new CompletionException("Unexpected error occurred", exception);
            }
        } , this.database.executorService());
    }

    @Override
    public CompletableFuture<Void> deleteByAAndB(int a, String b) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("delete from hello where a=? and b=?")) {
                    statement.setInt(1, a);
                    statement.setString(2, b);
                    statement.executeUpdate();
                    return null;
                }
            } catch (SQLException exception) {
                throw new CompletionException("Unexpected error occurred", exception);
            }
        } , this.database.executorService());
    }
}