buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:3.2.3")
        classpath("io.spring.gradle:dependency-management-plugin:1.1.4")
    }
}

// Apply plugins and configurations... 