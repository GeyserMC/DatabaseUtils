dependencies {
    api(projects.core)
    api(libs.hikari.cp)

    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
}