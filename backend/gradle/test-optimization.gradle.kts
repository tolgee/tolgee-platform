// Include this in modules that have tests
// Apply with: apply(from = "${rootDir}/gradle/test-optimization.gradle.kts")

// Enable build cache
buildCache {
    local {
        isEnabled = true
        directory = "${rootProject.buildDir}/build-cache"
        removeUnusedEntriesAfterDays = 7
    }
}

// Configure tests
tasks.withType<Test> {
    // Enable parallel test execution
    maxParallelForks = Math.max(1, Runtime.getRuntime().availableProcessors() / 2)
    
    // Optimize JVM memory settings
    jvmArgs = listOf(
        "-Xmx512m", 
        "-XX:MaxMetaspaceSize=256m",
        "-XX:+UseParallelGC",
        "-Djava.security.egd=file:/dev/./urandom" // Faster entropy source
    )
    
    // Fail fast on first test failure
    failFast = true
    
    // Reuse JVM for faster test execution
    forkEvery = 100
    
    // Enable test output on console
    testLogging {
        events("passed", "skipped", "failed")
        showExceptions = true
        showCauses = true
        showStackTraces = false
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
    }
    
    // Enable test caching
    outputs.upToDateWhen { true }
    
    // Filter tests based on system properties
    if (System.getProperty("test.single") != null) {
        filter {
            includeTestsMatching(System.getProperty("test.single"))
        }
    }
}

// Configure Java compilation
tasks.withType<JavaCompile> {
    options.isFork = true
    options.isIncremental = true
}

// Configure Kotlin compilation
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
} 