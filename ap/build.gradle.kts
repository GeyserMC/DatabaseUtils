dependencies {
    implementation(projects.core)
    implementation(projects.databaseCore)

    implementation(projects.databaseSql)
    implementation(libs.sqlbuilder)

    implementation(projects.databaseMongo)

    implementation(libs.javapoet)
    implementation(libs.auto.service)
    annotationProcessor(libs.auto.service)

    testImplementation(libs.compile.testing)
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.withType<Test>().configureEach {
    doFirst {
        // See: https://github.com/google/compile-testing/issues/222
        if (javaLauncher.get().metadata.languageVersion >= JavaLanguageVersion.of(9)) {
            jvmArgs(
                "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
                "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED"
            )
        }
    }
}