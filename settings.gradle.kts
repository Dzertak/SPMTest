pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("common") {
            from(files("gradle/common.versions.toml"))
        }
    }
}

rootProject.name = "SPMTest"
include(":shared")