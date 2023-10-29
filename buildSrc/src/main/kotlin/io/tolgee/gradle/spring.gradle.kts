package io.tolgee.gradle

plugins {
    id("io.spring.dependency-management")

    // Classes annotated with @Configuration, @Controller, @RestController, @Service or @Repository are automatically opened
    kotlin("plugin.spring")

    // Allows to package executable jar or war archives, run Spring Boot applications, and use the dependency management
    id("org.springframework.boot")
}

apply(plugin = "org.jetbrains.kotlin.plugin.spring")
apply(plugin = "org.springframework.boot")
apply(plugin = "io.spring.dependency-management")
