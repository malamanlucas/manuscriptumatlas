plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    application
}

group = "com.ntcoverage"
version = "1.0.0"

application {
    mainClass.set("com.ntcoverage.ApplicationKt")
}

repositories {
    mavenCentral()
}

val ktorVersion = "3.1.1"
val exposedVersion = "0.58.0"

dependencies {
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    implementation("org.postgresql:postgresql:42.7.5")
    implementation("com.zaxxer:HikariCP:6.2.1")

    implementation("org.flywaydb:flyway-core:11.3.0")
    implementation("org.flywaydb:flyway-database-postgresql:11.3.0")

    implementation("org.jsoup:jsoup:1.18.3")

    implementation("ch.qos.logback:logback-classic:1.5.16")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.1.10")
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.ntcoverage.ApplicationKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
