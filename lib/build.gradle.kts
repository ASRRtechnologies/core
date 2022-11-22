import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.4.31"
    kotlin("plugin.spring") version "1.4.21-2"

    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    `maven-publish`
    signing
    application
    jacoco
}

application {
    mainClass.set("nl.asrr.common.Library")
}

val springBootDependencyVersion = "2.6.1"
java.sourceCompatibility = JavaVersion.VERSION_11

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ASRRtechnologies/lib-asrr-common-kt")
            credentials {
                username = project.findProperty("github.username") as String? ?: System.getenv("GITHUB_USERNAME")
                password = project.findProperty("github.token") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            pom {
                name.set("ASRR Common Kotlin Libary")
                description.set("A library for all common ASRR code")
                url.set("https://www.asrr.nl")
                properties.set(
                    mapOf(
                        "version" to version
                    )
                )
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("Amar97")
                        name.set("Amar Ramdas")
                        email.set("amar.ramdas@asrr.nl")
                    }
                    developer {
                        id.set("vanishaov")
                        name.set("Vanisha Varma")
                        email.set("vanisha.varma@asrr.nl")
                    }
                }
            }
            groupId = "nl.asrr"
            artifactId = "common"

            from(components["java"])
        }
    }
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

java {
    withSourcesJar()
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // This dependency is used internally, and not exposed to consumers on their own compile classpath.
    implementation("com.google.guava:guava:30.0-jre")

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web:$springBootDependencyVersion")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb:$springBootDependencyVersion")
    implementation("org.springframework.boot:spring-boot-starter:$springBootDependencyVersion")
    implementation("org.springframework.boot:spring-boot-starter-security:$springBootDependencyVersion")
    implementation("org.springframework.boot:spring-boot-starter-validation:$springBootDependencyVersion")
    implementation("org.springframework.boot:spring-boot-starter-test:$springBootDependencyVersion")
    implementation("org.springdoc:springdoc-openapi-ui:1.5.11")
    // https://mvnrepository.com/artifact/com.github.oshi/oshi-core
    implementation("com.github.oshi:oshi-core:6.3.2")

    // Json web tokens for authentication
    implementation("io.jsonwebtoken:jjwt:0.9.1")

    // Lombok
    implementation("org.projectlombok:lombok:1.18.22")

    // Kotlin Logger
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")

    // User mockK
    testImplementation("com.ninja-squad:springmockk:3.0.1")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // This dependency is exported to consumers, that is to say found on their compile classpath.
    api("org.apache.commons:commons-math3:3.6.1")
}

tasks.jar {
    manifest {
        attributes(mapOf("Implementation-Title" to project.name,
            "Implementation-Version" to project.version))
    }
}

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
        xml.destination = file("$buildDir/reports/jacoco/report.xml")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    finalizedBy(tasks.jacocoTestReport)
    useJUnitPlatform()
}
