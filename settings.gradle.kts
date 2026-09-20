import java.util.Properties

// 1. Lee tu token secreto desde local.properties
val localProps = Properties().apply {
    val file = rootProject.projectDir.resolve("local.properties")
    if (file.exists()) {
        load(file.inputStream())
    }
}

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // 2. Repositorio de Mapbox donde se descarga el SDK usando tu token
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox"
                password = localProps.getProperty("MAPBOX_DOWNLOADS_TOKEN") ?: ""
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

rootProject.name = "trackerservicios"
include(":app")