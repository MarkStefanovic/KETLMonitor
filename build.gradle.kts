import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jmailen.kotlinter") version "3.9.0"
    id("org.jetbrains.compose") version "1.2.0-alpha01-dev629"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.6.20-M1"
    id("com.github.ben-manes.versions") version "0.42.0"
}

group = "me.mes"
version = "1.02"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    testImplementation(kotlin("test"))

    implementation(compose.desktop.currentOs)

    implementation("androidx.compose.compiler:compiler:1.2.0-alpha03")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")

    implementation("org.xerial:sqlite-jdbc:3.36.0.3")

    implementation("org.postgresql:postgresql:42.3.3")

    implementation("com.zaxxer:HikariCP:5.0.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
}

kotlinter {
    indentSize = 2
}

tasks {
    test {
        useJUnitPlatform()
    }
    compileKotlin {
        kotlinOptions.jvmTarget = "16"
    }
    compileJava {
        targetCompatibility = "16"
    }
}


compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            includeAllModules = true
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "KETLMonitor"
            packageVersion = "1.0.0"
        }
    }
}