This project tries to support the following SQL dialects:

- H2
- (Microsoft) SQL Server
- MySQL
- Oracle Database
- PostgreSQL
- SQLite

However not every data type has the same name / is available on each dialect.
These are the base type conversions:

| Java Type | SQL type         | Reason / remarks                                                |
|-----------|------------------|-----------------------------------------------------------------|
| Boolean   | boolean          | More platforms support boolean than tinyint / bit               |
| Byte      | smallint         | SQL Server's tinyint is unsigned and PostgresSQL has no tinyint |
| Short     | smallint         | -                                                               |
| Char      | smallint         | Basically the same as short                                     |
| Integer   | int              | -                                                               |
| Long      | bigint           | -                                                               |
| Float     | real             | Some dialects map this as a double precision type               |
| Double    | double precision | -                                                               |
| String    | varchar          | -                                                               |
| Byte[]    | varbinary        | Most dialects support varbinary                                 |

And these are the exceptions:

| Java Type | Dialect    | SQL Type |
|-----------|------------|----------|
| Boolean   | SQLite     | int      |
| Boolean   | SQL Server | bit      |
| Double    | SQL Server | float    |
| Byte[]    | PostgreSQL | bytea    |
| Byte[]    | SQLite     | varchar  |