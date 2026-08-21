import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val jvmTarget = "25"
val micronautVersion = "5.1.1"
val logbackEncoderVersion = "9.0"
val postgresqlVersion = "42.7.12"
val poiVersion = "5.5.1"
val rapidsRiversVersion = "202606190809"
val grunndataDtoVersion = "202607151330"
val microsoftGraphVersion = "5.77.0"
val leaderElectionVersion = "202606231046"
val googleCloudPlatformVersion = "26.61.0"
val azureIdentityVersion = "1.18.4"
val flywayPostgresqlVersion = "13.2.0"

// Security versions:
val jsonSmartVersion = "2.6.0"
val commonsCompressVersion = "1.28.0"
val opencsvVersion = "5.12.0"

group = "no.nav.hm"

plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.allopen") version "2.3.21"
    id("com.google.devtools.ksp") version "2.3.7"
    id("java")
    id("com.gradleup.shadow") version "9.6.1"
    id("io.micronaut.application") version "5.0.2"
    id("com.github.ben-manes.versions") version "0.61.0"

}

configurations.all {
    resolutionStrategy {
        failOnChangingVersions()
        force("commons-beanutils:commons-beanutils:1.11.0")
    }
}

dependencies {
    constraints {
        implementation("com.opencsv:opencsv:$opencsvVersion")
        implementation("commons-beanutils:commons-beanutils:1.11.0")
    }

    api("ch.qos.logback:logback-classic")
    api("net.logstash.logback:logstash-logback-encoder:$logbackEncoderVersion")

    runtimeOnly("org.yaml:snakeyaml")
    implementation("io.micronaut:micronaut-jackson-databind")
    implementation("tools.jackson.module:jackson-module-kotlin")

    // coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")

    // security
    implementation("io.micronaut.security:micronaut-security-jwt")
    ksp("io.micronaut.security:micronaut-security-annotations")
    ksp("io.micronaut:micronaut-inject-java")

    // micronaut-data
    implementation("io.micronaut.data:micronaut-data-jdbc")
    implementation("jakarta.persistence:jakarta.persistence-api")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("io.micronaut.sql:micronaut-jdbc-hikari")
    ksp("io.micronaut.data:micronaut-data-processor")
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation("io.micronaut.flyway:micronaut-flyway")
    implementation("io.micronaut:micronaut-runtime")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("io.micronaut:micronaut-http-server-netty")
    implementation("io.micronaut:micronaut-http-client")
    implementation("io.micronaut.cache:micronaut-cache-caffeine")
    implementation("io.micronaut.micrometer:micronaut-micrometer-core")
    implementation("io.micronaut.micrometer:micronaut-micrometer-registry-prometheus")
    implementation("io.micronaut:micronaut-management")
    testImplementation("io.mockk:mockk")
    testImplementation("io.kotest:kotest-assertions-core-jvm")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.testcontainers:testcontainers-postgresql")

    // Rapids and Rivers
    implementation("com.github.navikt:hm-rapids-and-rivers-v2-core:$rapidsRiversVersion")
    implementation("com.github.navikt:hm-rapids-and-rivers-v2-micronaut:$rapidsRiversVersion")
    implementation("com.github.navikt:hm-rapids-and-rivers-v2-micronaut-deadletter:$rapidsRiversVersion")
    implementation("no.nav.hm.grunndata:hm-grunndata-rapid-dto:$grunndataDtoVersion")

    // OpenApi
    implementation("io.micronaut.openapi:micronaut-openapi")
    compileOnly("io.micronaut.openapi:micronaut-openapi-annotations")

    // excel import
    implementation("org.apache.poi:poi:$poiVersion")
    implementation("org.apache.poi:poi-ooxml:$poiVersion")

    // Microsoft Graph
    implementation("com.microsoft.graph:microsoft-graph:$microsoftGraphVersion")
    implementation("com.azure:azure-identity:$azureIdentityVersion")
    implementation("net.minidev:json-smart:$jsonSmartVersion")

    // flyway-postgresql
    implementation("org.flywaydb:flyway-database-postgresql:$flywayPostgresqlVersion")

    // micronaut-leaderelection
    implementation("com.github.navikt:hm-micronaut-leaderelection:$leaderElectionVersion")

    implementation("org.apache.commons:commons-compress:$commonsCompressVersion")

    implementation("com.opencsv:opencsv:$opencsvVersion")

    implementation(platform("com.google.cloud:libraries-bom:$googleCloudPlatformVersion"))
    implementation("com.google.cloud:google-cloud-vertexai")
}

micronaut {
    version.set(micronautVersion)
    testRuntime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
    }
}

application {
    mainClass.set("no.nav.hm.grunndata.register.Application")
}

java {
    sourceCompatibility = JavaVersion.toVersion(jvmTarget)
    targetCompatibility = JavaVersion.toVersion(jvmTarget)
    withSourcesJar()
}

tasks.withType<KotlinCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(jvmTarget))
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(jvmTarget))
}

tasks.named<ShadowJar>("shadowJar") {
    isZip64 = true
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("skipped", "failed")
        showExceptions = true
        showStackTraces = true
        showCauses = true
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = true
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}
