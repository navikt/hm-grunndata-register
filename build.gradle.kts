import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val jvmTarget = "17"
val micronautVersion="3.7.4"
val kafkaVersion = "3.2.1"
val micrometerRegistryPrometheusVersion = "1.9.1"
val junitJupiterVersion = "5.9.0"
val jacksonVersion = "2.13.4"
val logbackClassicVersion = "1.2.11"
val logbackEncoderVersion = "7.2"
val kafkaEmbeddedVersion = "3.2.1"
val postgresqlVersion= "42.3.3"
val tcVersion= "1.16.3"
val mockkVersion = "1.13.2"
val kotestVersion = "5.5.0"
val apachePoiVersion = "5.2.3"
val openSearchRestClientVersion = "1.3.5"

group = "no.nav.hm"
version = properties["version"] ?: "local-build"

plugins {
    kotlin("jvm") version "1.7.0"
    kotlin("kapt") version "1.7.0"
    kotlin("plugin.allopen") version "1.7.0"
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("io.micronaut.application") version "3.6.5"
}

configurations.all {
    resolutionStrategy {
        failOnChangingVersions()
    }
}

dependencies {
    //implementation("com.github.navikt:hm-rapids-and-rivers-v2-core:1.0-SNAPSHOT")
    //implementation("com.github.navikt:hm-rapids-and-rivers-v2-micronaut:1.0-SNAPSHOT")
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
    implementation("jakarta.persistence:jakarta.persistence-api:2.2.3")
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
    // Apache POI for excel file handling
    implementation("org.apache.poi:poi:$apachePoiVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.micronaut.test:micronaut-test-kotest5")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.0")
    testImplementation("no.nav:kafka-embedded-env:$kafkaEmbeddedVersion")
    {
        exclude("log4j")
        exclude("org.glassfish")
        exclude("io.netty")
    }
    testImplementation("org.testcontainers:postgresql:${tcVersion}")
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

}

