plugins {
    application
}

dependencies {
    implementation(project(":common"))
    implementation(libs.pekko.connectors.udp)
    runtimeOnly(libs.logback.classic)
}

application {
    applicationDefaultJvmArgs = listOf("--sun-misc-unsafe-memory-access=allow")
    mainClass = "app.warehouse.ApplicationKt"
}
