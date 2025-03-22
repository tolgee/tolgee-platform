plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    kotlin("kapt")
}

dependencies {
    implementation(project(":common"))
    
    // Spring Boot dependencies
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    
    // Database
    implementation("org.postgresql:postgresql")
    implementation("com.h2database:h2")
    
    // Use Testcontainers for isolated testing
    testImplementation("org.testcontainers:postgresql:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.springframework.security:spring-security-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
    
    // Optimize test execution
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
    
    // Enable test output for debugging
    testLogging {
        events("passed", "skipped", "failed")
    }
    
    // Add system properties for test optimization
    systemProperty("spring.jpa.hibernate.ddl-auto", "create-drop")
    systemProperty("spring.datasource.url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
    systemProperty("spring.datasource.driver-class-name", "org.h2.Driver")
    systemProperty("spring.jpa.properties.hibernate.dialect", "org.hibernate.dialect.H2Dialect")
    systemProperty("spring.jpa.properties.hibernate.format_sql", "false")
    systemProperty("spring.jpa.properties.hibernate.show_sql", "false")
    systemProperty("logging.level.org.hibernate.SQL", "ERROR")
    systemProperty("logging.level.org.hibernate.type.descriptor.sql.BasicBinder", "ERROR")
} 