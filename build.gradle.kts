plugins {
    alias(libs.plugins.indra)
    alias(libs.plugins.indra.publishing) apply false
    alias(libs.plugins.indra.licenser.spotless)
}

group = "org.geysermc.databaseutils"

subprojects {
    apply {
        plugin("net.kyori.indra")
        plugin("net.kyori.indra.publishing")
        plugin("net.kyori.indra.licenser.spotless")
    }

    indra {
        github("GeyserMC", "DatabaseUtils") {
            ci(true)
            issues(true)
            scm(true)
        }

        mitLicense()

        javaVersions { target(17) }

        publishSnapshotsTo("geysermc", "https://repo.opencollab.dev/maven-snapshots")
        publishReleasesTo("geysermc", "https://repo.opencollab.dev/maven-releases")
    }

    spotless {
        java {
            palantirJavaFormat()
            formatAnnotations()
        }
        ratchetFrom("origin/main")
    }
}