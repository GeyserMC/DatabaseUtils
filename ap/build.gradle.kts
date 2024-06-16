dependencies {
    implementation(projects.core)

    implementation(projects.databaseSql)

    implementation(projects.databaseMongo)

    implementation(libs.javapoet)
    implementation(libs.auto.service)
    annotationProcessor(libs.auto.service)

    testImplementation(libs.compile.testing)
    testImplementation(libs.bundles.junit)
    testRuntimeOnly(libs.junit.engine)
}

tasks.withType<Test>().configureEach {
    doFirst {
        // See: https://github.com/google/compile-testing/issues/222
        jvmArgs(
            "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
            "--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
            "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
        )
    }
}