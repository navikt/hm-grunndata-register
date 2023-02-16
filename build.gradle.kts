import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val jvmTarget = "17"
val micronautVersion="3.8.4"
val kafkaVersion = "3.2.1"
val micrometerRegistryPrometheusVersion = "1.9.1"
val junitJupiterVersion = "5.9.0"
val jacksonVersion = "2.13.4"
val logbackClassicVersion = "1.4.5"
val logbackEncoderVersion = "7.2"
val kafkaEmbeddedVersion = "3.2.1"
val postgresqlVersion= "42.5.1"
val tcVersion= "1.17.6"
val mockkVersion = "1.13.2"
val kotestVersion = "5.5.4"
val openSearchRestClientVersion = "1.3.5"
val rapidsRiversVersion="202301310932"
val grunndataDtoVersion = "202302161342"

group = "no.nav.hm"
version = properties["version"] ?: "local-build"

plugins {
    kotlin("jvm") version "1.7.0"
    kotlin("kapt") version "1.7.0"
    kotlin("plugin.allopen") version "1.7.0"
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("io.micronaut.application") version "3.6.6"
}

configurations.all {
    resolutionStrategy {
        failOnChangingVersions()
    }
}

dependencies {

    api("ch.qos.logback:logback-classic:$logbackClassicVersion")
    api("net.logstash.logback:logstash-logback-encoder:$logbackEncoderVersion")
    // coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")
    // security
    implementation("io.micronaut.security:micronaut-security-jwt")
    annotationProcessor("io.micronaut.security:micronaut-security-annotations")

    // micronaut-data
    implementation("io.micronaut.data:micronaut-data-jdbc")
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("io.micronaut.sql:micronaut-jdbc-hikari")
    annotationProcessor("io.micronaut.data:micronaut-data-processor")
    implementation("org.postgresql:postgresql:${postgresqlVersion}")
    implementation("io.micronaut.flyway:micronaut-flyway")
    implementation("io.micronaut:micronaut-runtime")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("io.micronaut:micronaut-http-server-netty")
    implementation("io.micronaut:micronaut-http-client")
    implementation("io.micronaut.cache:micronaut-cache-caffeine")
    implementation("io.micronaut.micrometer:micronaut-micrometer-core")
    implementation("io.micronaut.micrometer:micronaut-micrometer-registry-prometheus")
    implementation("io.micronaut:micronaut-management")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.micronaut.test:micronaut-test-kotest5")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.0")
    testImplementation("org.testcontainers:postgresql:${tcVersion}")

    // Rapids and Rivers
    implementation("com.github.navikt:hm-rapids-and-rivers-v2-core:$rapidsRiversVersion")
    implementation("com.github.navikt:hm-rapids-and-rivers-v2-micronaut:$rapidsRiversVersion")
    implementation("no.nav.hm.grunndata:hm-grunndata-rapid-dto:$grunndataDtoVersion")
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
    kotlinOptions.jvmTarget = jvmTarget
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions.jvmTarget = jvmTarget
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

tasks.withType<Wrapper> {
    gradleVersion = "7.5.1"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")

}

