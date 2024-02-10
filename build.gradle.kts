plugins {
    alias(libs.plugins.indra)
    alias(libs.plugins.indra.publishing)
    alias(libs.plugins.indra.licenser.spotless)
}

group = "org.geysermc.databaseutils"

subprojects {
    apply {
        plugin("java-library")
        plugin("net.kyori.indra")
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
    }

    spotless {
        java {
            palantirJavaFormat()
            formatAnnotations()
        }
        ratchetFrom("origin/main")
    }
}