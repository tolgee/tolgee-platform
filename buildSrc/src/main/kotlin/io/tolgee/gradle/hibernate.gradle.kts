package io.tolgee.gradle

plugins {
    id("base")
    id("kotlin")
    id("org.jetbrains.kotlin.plugin.jpa")
    id("org.hibernate.orm")
}

//apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
//apply(plugin = "org.hibernate.orm")

configure<org.jetbrains.kotlin.allopen.gradle.AllOpenExtension> {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.MappedSuperclass")
    annotation("javax.persistence.Embeddable")
}
