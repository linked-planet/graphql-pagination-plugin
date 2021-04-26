import org.gradle.util.GradleVersion

println("Gradle Version: " + GradleVersion.current().toString())

group = "com.linked-planet.plugin"
version = "0.1.0-SNAPSHOT"

ext.set("kotlinVersion", "1.4.10")
ext.set("jvmTarget", "1.8")

plugins {
    id("com.github.hierynomus.license") version "0.15.0"
    id("com.github.hierynomus.license-report") version "0.15.0"
    id("com.github.ben-manes.versions") version "0.21.0"
    id("org.jetbrains.kotlin.jvm") version "1.4.10" apply false
}

subprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://dl.bintray.com/arrow-kt/arrow-kt/") }
        maven { url = uri("https://dl.bintray.com/kotlin/kotlinx.html") }
        maven { url = uri("https://dl.bintray.com/kotlin/ktor") }
        maven { url = uri("https://dl.bintray.com/kotlin/kotlin-js-wrappers") }
        maven { url = uri("https://kotlin.bintray.com/kotlinx") }
        maven { url = uri("https://dl.bintray.com/kotlin/kotlin-eap") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    }
}
