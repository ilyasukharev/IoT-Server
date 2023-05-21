import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val exposed_version: String by project
val postgresql_version: String by project
val kreds_version: String by project
val koin_ktor_version: String by project
val apache_email_version: String by project
val tomcat_version: String by project

plugins {
    application
    kotlin("jvm") version "1.7.22"
    id("io.ktor.plugin") version "2.2.3"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.22"
}

group = "com.iotserv"
version = "0.0.1"
application {
    mainClass.set("com.iotserv.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

ktor {
    fatJar {
        archiveFileName.set("iot-server.jar")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

dependencies {
    // https://mvnrepository.com/artifact/io.ktor/ktor-server-tomcat
// https://mvnrepository.com/artifact/io.ktor/ktor-server-jetty
    implementation("io.ktor:ktor-server-jetty:1.3.2-1.4-M2")

    implementation ("io.bkbn:kompendium-resources:latest.release")
    implementation("io.bkbn:kompendium-core:latest.release")
    implementation("org.apache.commons:commons-email:$apache_email_version")
    implementation("io.ktor:ktor-server-request-validation:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")
    implementation ("io.insert-koin:koin-ktor:$koin_ktor_version")
    implementation ("io.insert-koin:koin-logger-slf4j:$koin_ktor_version")
    implementation("io.github.crackthecodeabhi:kreds:$kreds_version")
    implementation("io.ktor:ktor-server-websockets:$ktor_version")
    implementation("org.postgresql:postgresql:$postgresql_version")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("io.ktor:ktor-server-websockets:$ktor_version")
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-resources:$ktor_version")
    implementation("io.ktor:ktor-server-host-common-jvm:2.2.1")
    implementation("io.ktor:ktor-server-status-pages-jvm:2.2.1")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}
