// Add this to ensure Kotlin uses Java 17
kotlin {
    jvmToolchain(17)
}

tasks.withType<Test> {
    // Enable parallel test execution
    maxParallelForks = Math.max(1, Runtime.getRuntime().availableProcessors() / 2)
    
    // Optimize JVM memory settings
    jvmArgs = listOf("-Xmx512m", "-XX:MaxMetaspaceSize=256m")
    
    // Fail fast on first test failure
    failFast = true
    
    // Reuse JVM for faster test execution
    forkEvery = 100
    
    // Enable test output on console
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Task for running fast tests
tasks.register<Test>("fastTest") {
    description = "Runs only fast tests for quick feedback"
    group = "verification"
    
    useJUnitPlatform {
        includeTags("fast")
    }
    
    // Same optimizations as regular tests
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
    jvmArgs = listOf("-Xmx512m", "-XX:MaxMetaspaceSize=256m")
    failFast = true
    
    testLogging {
        events("passed", "skipped", "failed")
    }
}

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "3.2.3"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
    kotlin("plugin.jpa") version "1.9.22"
    kotlin("kapt") version "1.9.22"
}

allprojects {
    repositories {
        mavenCentral()
    }
    
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "17"
        }
    }
    
    tasks.withType<Test> {
        useJUnitPlatform()
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    
    // Use Hikari connection pool with optimized settings
    implementation("com.zaxxer:HikariCP")
    
    // Testing dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("io.mockk:mockk:1.13.9")
}

tasks.withType<BootJar> {
    enabled = false
} 