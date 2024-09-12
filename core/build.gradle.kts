dependencies {
    compileOnlyApi(libs.hikari.cp)
    compileOnlyApi(libs.mongo.driver)
    compileOnly(libs.checker.qual)

    testAnnotationProcessor(projects.ap)
    testImplementation(projects.databaseSql)
    testImplementation(projects.databaseMongo)

    // database drivers
    testRuntimeOnly(libs.h2)
    testRuntimeOnly(libs.mariadb)
    testRuntimeOnly(libs.mongo.driver)
    testRuntimeOnly(libs.mssql.jdbc)
    testRuntimeOnly(libs.mysql)
    testRuntimeOnly(libs.oracle)
    testRuntimeOnly(libs.postgresql)
    testRuntimeOnly(libs.sqlite)
    // database containers
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.mariadb)
    testImplementation(libs.testcontainers.mongodb)
    testImplementation(libs.testcontainers.mssqlserver)
    testImplementation(libs.testcontainers.mysql)
    testImplementation(libs.testcontainers.oracle)
    testImplementation(libs.testcontainers.postgresql)

    testImplementation(libs.bundles.junit)
    testRuntimeOnly(libs.junit.engine)
}