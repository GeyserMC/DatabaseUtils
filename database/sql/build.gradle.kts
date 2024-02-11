dependencies {
    api(projects.databaseCore)
    api(libs.hikari.cp)

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}