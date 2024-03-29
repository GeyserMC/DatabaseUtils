dependencies {
    compileOnlyApi(libs.hikari.cp)
    compileOnly(libs.checker.qual)

    testImplementation(projects.databaseSql)
    testRuntimeOnly(libs.h2)
    testAnnotationProcessor(projects.ap)

    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
}