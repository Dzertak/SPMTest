import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("com.chromaticnoise.multiplatform-swiftpackage") version "2.0.3"
    id("maven-publish")
}

group = common.versions.library.group.get()
version = common.versions.library.version.get()

var androidTarget: String = ""

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    targetHierarchy.default()



    val android = android {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
        publishLibraryVariants("release")
    }
    androidTarget = android.name

    val xcf = XCFramework()
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            xcf.add(this)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                //put your multiplatform dependencies here
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by getting {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {

            }
        }

    }

    multiplatformSwiftPackage {
        packageName("shared")
        swiftToolsVersion("5.3")
        targetPlatforms {
            iOS { v("13") }
        }
        outputDirectory(File(rootDir, "/"))
    }
}

android {
    namespace = "com.dzertak.spmtest"
    compileSdk = 33
    defaultConfig {
        minSdk = 24
    }

    beforeEvaluate {
        libraryVariants.all {
            compileOptions {
                // Flag to enable support for the new language APIs
                isCoreLibraryDesugaringEnabled = true
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
    dependencies {
        //coreLibraryDesugaring(commonlibs.versions.compileSdk.get() as String)//same as with compileSdk
        coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")//same as with compileSdk
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    buildToolsVersion = "33.0.1"
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

fun readProperties(propertiesFile: File) = Properties().apply {
    propertiesFile.inputStream().use { fis ->
        load(fis)
    }
}

publishing {
    val properties = readProperties(project.rootProject.file("local.properties"))
    repositories {
        maven {
            name = "spmtest"
            url = uri("https://maven.pkg.github.com/dzertak/spmtest")
            credentials {
                username = System.getenv("USERNAME") ?: properties.getProperty("GITHUB_ID")
                password = System.getenv("PASSWORD") ?:properties.getProperty("GITHUB_PACKAGES_TOKEN")
            }
        }
    }
    val thePublications = listOf(androidTarget) + "kotlinMultiplatform"
    publications {
        matching { it.name in thePublications }.all {
            val targetPublication = this@all
            tasks.withType<AbstractPublishToMaven>()
                .matching { it.publication == targetPublication }
                .configureEach { onlyIf { findProperty("isMainHost") == "true" } }
        }
        matching { it.name.contains("ios", true) }.all {
            val targetPublication = this@all
            tasks.withType<AbstractPublishToMaven>()
                .matching { it.publication == targetPublication }
                .forEach { it.enabled = false }
        }
    }
}