dependencies {
    compileOnlyApi(libs.hikari.cp)
    compileOnly(libs.checker.qual)

    testImplementation(projects.databaseSql)
    testRuntimeOnly(libs.h2)
    testAnnotationProcessor(projects.ap)

    testImplementation(libs.bundles.junit)
    testRuntimeOnly(libs.junit.engine)
}