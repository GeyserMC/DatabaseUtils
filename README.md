DatabaseUtils is a library that allows you to write Repository interfaces that support both SQL and No-SQL (MongoDB) databases.
The implementation of the interface is automatically generated while compiling.

This readme will be expanded in the future with for example code examples,
currently examples can be found in the tests of the AP module and the tests of the core module. 

# What's left to do?
- make 'simple' actions like `insert` more flexible
  - allow it to return whether there was a row added
  - support adding every variable of the entity as parameter
- add `save` which either inserts the entity if it's not present or updates the already existing entity
- adding migrations
- and plenty more

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
- MariaDB
- MySQL
- Oracle Database
- PostgreSQL
- SQLite

However not everything is the same across dialects.

### Missing functionality for specific dialects

#### TestEntity deleteByAAndB(String, String)
This behaviour doesn't work on MySQL (not MariaDB) and H2.
This is because every dialect but MySQL and H2 support 'REPLACING' or a variant of it.
For those two dialect we have to fetch a record first and then delete it (inside a transaction).
The current codebase is not flexible enough to do these wildly different behaviours per dialect, 
but will be supported in the future. 

### Different data types across dialects
Not every data type has the same name / is available on each dialect.
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