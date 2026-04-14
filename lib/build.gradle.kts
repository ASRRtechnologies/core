import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("org.springframework.boot") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jetbrains.kotlin.jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"

    `java-library`
    `maven-publish`
    signing
    application
    jacoco
    id("io.github.sgtsilvio.gradle.maven-central-publishing") version "0.4.1"
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}

application {
    mainClass.set("nl.asrr.core.Library")
}

java.sourceCompatibility = JavaVersion.VERSION_21

java {
    withJavadocJar()
    withSourcesJar()
}

signing {
    val password = findProperty("signing.password")?.toString() ?: System.getenv("GPG_PASSPHRASE")
    val secretKey = findProperty("signing.key")?.toString() ?: System.getenv("GPG_PRIVATE_KEY")

    useInMemoryPgpKeys(secretKey, password)
    sign(publishing.publications)
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            groupId = "nl.asrr"
            artifactId = "core"

            from(components["java"])

            // Write resolved versions into the published pom so Maven Central
            // validation passes (BOM-managed deps otherwise lack versions).
            versionMapping {
                usage("java-api") { fromResolutionOf("runtimeClasspath") }
                usage("java-runtime") { fromResolutionResult() }
            }

            pom {
                name.set("ASRR Core Kotlin Library")
                description.set("A library for all common ASRR code")
                url.set("https://www.asrr.nl")
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
                scm {
                    connection.set("scm:git:git://github.com/ASRRtechnologies/core.git")
                    developerConnection.set("scm:git:ssh://github.com/ASRRtechnologies/core.git")
                    url.set("https://github.com/ASRRtechnologies/core")
                }
            }
        }
    }
}

dependencies {
    implementation("com.github.oshi:oshi-core:6.8.2")

    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("com.google.guava:guava:33.4.0-jre")

    // Spring Boot (versions managed by the Spring Boot BOM via the dependency-management plugin)
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")

    // JWT — modern jjwt API. impl/jackson modules are runtime-only.
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Kotlin Logging — new coordinates as of v3+
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.14")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    api("org.apache.commons:commons-math3:3.6.1")
}

tasks.jar {
    manifest {
        attributes(mapOf("Implementation-Title" to project.name,
            "Implementation-Version" to project.version))
    }
}

// This is a library, not an app: don't produce/require a Spring Boot fat jar.
tasks.named("bootJar") { enabled = false }
tasks.named("jar") { enabled = true }

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        xml.outputLocation.set(file("${layout.buildDirectory.get().asFile}/reports/jacoco/report.xml"))
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.withType<Test> {
    finalizedBy(tasks.jacocoTestReport)
    useJUnitPlatform()
}
