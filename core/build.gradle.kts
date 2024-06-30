dependencies {
    compileOnlyApi(libs.hikari.cp)
    compileOnlyApi(libs.mongo.driver)
    compileOnly(libs.checker.qual)

    testImplementation(projects.databaseSql)
    testImplementation(projects.databaseMongo)
    testRuntimeOnly(libs.h2)
    testRuntimeOnly(libs.mongo.driver)
    testAnnotationProcessor(projects.ap)

    testImplementation(libs.bundles.junit)
    testRuntimeOnly(libs.junit.engine)
}