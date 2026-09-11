import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

val catalog = libs

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = "app"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }

    dependencies {
        add("implementation", platform(catalog.pekko.bom))
        add("testImplementation", catalog.bundles.test)
        add("testRuntimeOnly", catalog.junit.platform.launcher)
    }

    extensions.configure<KotlinJvmProjectExtension> {
        jvmToolchain(catalog.versions.jvm.get().toInt())
    }

    tasks.withType<Test> {
        jvmArgs("--sun-misc-unsafe-memory-access=allow")
        useJUnitPlatform()
    }
}

tasks.withType<Wrapper> {
    gradleVersion = catalog.versions.gradle.get()
}
