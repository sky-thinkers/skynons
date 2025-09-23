plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
}

group = "com.skythinkers"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}


dependencies {
    implementation(projects.shared)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    implementation("io.ktor:ktor-client-core")
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)

}

tasks.register<Exec>("setupBackedCompiler") {
    commandLine("cmake", "-DCMAKE_BUILD_TYPE=Debug", "-Bbuild")
    workingDir(file("../backend"))
}

tasks.register<Exec>("compileBackend") {
    dependsOn(tasks["setupBackedCompiler"])
    commandLine("cmake", "--build", "build", "-j", "8")
    workingDir(file("../backend"))
}

tasks.named<Task>("build") {
    dependsOn(tasks["compileBackend"])
}
