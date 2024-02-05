dependencies {
    implementation(projects.databaseCore)
    compileOnly(libs.checker.qual)

    testImplementation(projects.databaseSql)
    testRuntimeOnly(libs.h2)
    testAnnotationProcessor(projects.ap)

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}