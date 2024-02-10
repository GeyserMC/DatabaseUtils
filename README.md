DatabaseUtils is a library that allows you to write Repository interfaces that support both SQL and No-SQL (MongoDB) databases.
The implementation of the interface is automatically generated while compiling.

This readme will be expanded in the future with for example code examples,
currently examples can be found in the tests of the AP module and the tests of the core module. 

# Supported types
Every database type is responsible for providing support for at least:
- Boolean
- Byte
- Short
- Char
- Integer
- Long
- Float
- Double
- String
- Byte[]

Using TypeCodecRegistry the supported types can be expanded (each of them will be stored as a byte[]).
The following types are added out of the box:
- UUID

# SQL

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