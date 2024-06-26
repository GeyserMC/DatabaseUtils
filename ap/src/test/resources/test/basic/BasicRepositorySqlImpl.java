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
import java.util.List;
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
    public CompletableFuture<List<TestEntity>> find() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection __connection = this.dataSource.getConnection()) {
                try (PreparedStatement __statement = __connection.prepareStatement("select * from hello")) {
                    try (ResultSet __result = __statement.executeQuery()) {
                        List<TestEntity> __responses = new java.util.ArrayList<>();
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
        } , this.database.executorService());
    }

    @Override
    public CompletableFuture<TestEntity> findByA(int a) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection __connection = this.dataSource.getConnection()) {
                try (PreparedStatement __statement = __connection.prepareStatement("select * from hello where a=?")) {
                    __statement.setInt(1, a);
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
    public CompletableFuture<Boolean> exists() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection __connection = this.dataSource.getConnection()) {
                try (PreparedStatement __statement = __connection.prepareStatement("select 1 from hello")) {
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
    public CompletableFuture<Boolean> existsByB(String b) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection __connection = this.dataSource.getConnection()) {
                try (PreparedStatement __statement = __connection.prepareStatement("select 1 from hello where b=?")) {
                    __statement.setString(1, b);
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
    public void update(List<TestEntity> entity) {
        try (Connection __connection = this.dataSource.getConnection()) {
            try (PreparedStatement __statement = __connection.prepareStatement("update hello set c=?,d=? where a=? and b=?")) {
                int __count = 0;
                for (var __element : entity) {
                    __statement.setString(1, __element.c());
                    __statement.setBytes(2, this.__d.encode(__element.d()));
                    __statement.setInt(3, __element.a());
                    __statement.setString(4, __element.b());
                    __statement.addBatch();
                    if (__count % 250 == 0) {
                        __statement.executeBatch();
                    }
                }
                __statement.executeBatch();
                __connection.commit();
                return;
            } catch (SQLException __exception) {
                __connection.rollback();
                throw __exception;
            }
        } catch (SQLException __exception) {
            throw new CompletionException("Unexpected error occurred", __exception);
        }
    }

    @Override
    public void update(TestEntity entity) {
        try (Connection __connection = this.dataSource.getConnection()) {
            try (PreparedStatement __statement = __connection.prepareStatement("update hello set c=?,d=? where a=? and b=?")) {
                __statement.setString(1, entity.c());
                __statement.setBytes(2, this.__d.encode(entity.d()));
                __statement.setInt(3, entity.a());
                __statement.setString(4, entity.b());
                __statement.executeUpdate();
                return;
            }
        } catch (SQLException __exception) {
            throw new CompletionException("Unexpected error occurred", __exception);
        }
    }

    @Override
    public CompletableFuture<Void> insert(TestEntity entity) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection __connection = this.dataSource.getConnection()) {
                try (PreparedStatement __statement = __connection.prepareStatement("insert into hello (a,b,c,d) values (?,?,?,?)")) {
                    __statement.setInt(1, entity.a());
                    __statement.setString(2, entity.b());
                    __statement.setString(3, entity.c());
                    __statement.setBytes(4, this.__d.encode(entity.d()));
                    __statement.executeUpdate();
                    return null;
                }
            } catch (SQLException __exception) {
                throw new CompletionException("Unexpected error occurred", __exception);
            }
        } , this.database.executorService());
    }

    @Override
    public void insert(List<TestEntity> entities) {
        try (Connection __connection = this.dataSource.getConnection()) {
            try (PreparedStatement __statement = __connection.prepareStatement("insert into hello (a,b,c,d) values (?,?,?,?)")) {
                int __count = 0;
                for (var __element : entities) {
                    __statement.setInt(1, __element.a());
                    __statement.setString(2, __element.b());
                    __statement.setString(3, __element.c());
                    __statement.setBytes(4, this.__d.encode(__element.d()));
                    __statement.addBatch();
                    if (__count % 250 == 0) {
                        __statement.executeBatch();
                    }
                }
                __statement.executeBatch();
                __connection.commit();
                return ;
            } catch (SQLException __exception) {
                __connection.rollback();
                throw __exception;
            }
        } catch (SQLException __exception) {
            throw new CompletionException("Unexpected error occurred", __exception);
        }
    }

    @Override
    public CompletableFuture<Void> delete(TestEntity entity) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection __connection = this.dataSource.getConnection()) {
                try (PreparedStatement __statement = __connection.prepareStatement("delete from hello where a=? and b=?")) {
                    __statement.setInt(1, entity.a());
                    __statement.setString(2, entity.b());
                    __statement.executeUpdate();
                    return null;
                }
            } catch (SQLException __exception) {
                throw new CompletionException("Unexpected error occurred", __exception);
            }
        } , this.database.executorService());
    }

    @Override
    public void delete(List<TestEntity> entities) {
        try (Connection __connection = this.dataSource.getConnection()) {
            try (PreparedStatement __statement = __connection.prepareStatement("delete from hello where a=? and b=?")) {
                int __count = 0;
                for (var __element : entities) {
                    __statement.setInt(1, __element.a());
                    __statement.setString(2, __element.b());
                    __statement.addBatch();
                    if (__count % 250 == 0) {
                        __statement.executeBatch();
                    }
                }
                __statement.executeBatch();
                __connection.commit();
                return ;
            } catch (SQLException __exception) {
                __connection.rollback();
                throw __exception;
            }
        } catch (SQLException __exception) {
            throw new CompletionException("Unexpected error occurred", __exception);
        }
    }

    @Override
    public CompletableFuture<Void> deleteByAAndB(int a, String b) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection __connection = this.dataSource.getConnection()) {
                try (PreparedStatement __statement = __connection.prepareStatement("delete from hello where a=? and b=?")) {
                    __statement.setInt(1, a);
                    __statement.setString(2, b);
                    __statement.executeUpdate();
                    return null;
                }
            } catch (SQLException __exception) {
                throw new CompletionException("Unexpected error occurred", __exception);
            }
        } , this.database.executorService());
    }
}