@file:Suppress("UnstableApiUsage")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "databaseutils-parent"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://repo.opencollab.dev/main")
    }
}

include(":core")
include(":ap")

arrayOf("mongo", "sql").forEach {
    val id = ":database-$it"
    include(id)
    project(id).projectDir = file("database/$it")
}
