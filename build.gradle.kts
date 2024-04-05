import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

group = "me.cpele.smalldata"

version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

allprojects {
    tasks.withType(KotlinCompile::class) { kotlinOptions.freeCompilerArgs += "-Xcontext-receivers" }
}
