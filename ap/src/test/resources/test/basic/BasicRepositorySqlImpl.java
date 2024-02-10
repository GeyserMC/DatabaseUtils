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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.geysermc.databaseutils.sql.SqlDatabase;

public final class BasicRepositorySqlImpl implements BasicRepository {
    private final SqlDatabase database;

    private final HikariDataSource dataSource;

    public BasicRepositorySqlImpl(SqlDatabase database) {
        this.database = database;
        this.dataSource = database.dataSource();
    }

    @Override
    public CompletableFuture<TestEntity> findByAAndB(int a, String b) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("select * from hello where a=? and b=?")) {
                    statement.setInt(1, a);
                    statement.setString(2, b);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            return null;
                        }
                        Integer _a = result.getInt("a");
                        String _b = result.getString("b");
                        String _c = result.getString("c");
                        return new TestEntity(_a, _b, _c);
                    }
                }
            } catch (SQLException exception) {
                throw new CompletionException("Unexpected error occurred", exception);
            }
        } , this.database.executorService());
    }

    @Override
    public CompletableFuture<Boolean> existsByAOrB(int a, String b) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("select 1 from hello where a=? or b=?")) {
                    statement.setInt(1, a);
                    statement.setString(2, b);
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
    public CompletableFuture<Void> update(TestEntity entity) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("update hello set c=? where a=? and b=?")) {
                    statement.setString(1, entity.c());
                    statement.setInt(2, entity.a());
                    statement.setString(3, entity.b());
                    statement.executeUpdate();
                    return null;
                }
            } catch (SQLException exception) {
                throw new CompletionException("Unexpected error occurred", exception);
            }
        } , this.database.executorService());
    }

    @Override
    public CompletableFuture<Void> insert(TestEntity entity) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement("insert into hello (a,b,c) values (?,?,?)")) {
                    statement.setInt(1, entity.a());
                    statement.setString(2, entity.b());
                    statement.setString(3, entity.c());
                    statement.executeUpdate();
                    return null;
                }
            } catch (SQLException exception) {
                throw new CompletionException("Unexpected error occurred", exception);
            }
        } , this.database.executorService());
    }
}