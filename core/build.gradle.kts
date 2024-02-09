plugins {
    kotlin("jvm")
}

group = "me.cpele.smalldata"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:+")
}