package io.tolgee.gradle

plugins {
    id("base")
    id("kotlin")
    id("io.spring.dependency-management")

    // Classes annotated with @Configuration, @Controller, @RestController, @Service or @Repository are automatically opened
    kotlin("plugin.spring")

    // Allows to package executable jar or war archives, run Spring Boot applications, and use the dependency management
    id("org.springframework.boot")
}

//apply(plugin = "org.jetbrains.kotlin.plugin.spring")
//apply(plugin = "org.springframework.boot")
//apply(plugin = "io.spring.dependency-management")

configure<org.jetbrains.kotlin.allopen.gradle.AllOpenExtension> {
    annotation("org.springframework.stereotype.Component")
    annotation("org.springframework.stereotype.Service")
    annotation("org.springframework.transaction.annotation.Transactional")
    annotation("org.springframework.beans.factory.annotation.Configurable")
    annotation("org.springframework.boot.test.context.SpringBootTest")
}
