dependencies {
    api(projects.core)
    api(libs.mongo.driver)

    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
}