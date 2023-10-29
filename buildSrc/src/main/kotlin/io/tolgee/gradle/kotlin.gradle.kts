package io.tolgee.gradle

import org.gradle.kotlin.dsl.kotlin

plugins {
    java
    kotlin("jvm")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.allopen")
    id("org.jlleitschuh.gradle.ktlint")
}

apply(plugin = "kotlin")
apply(plugin = "org.jetbrains.kotlin.kapt")
apply(plugin = "org.jetbrains.kotlin.plugin.allopen")

kotlin {
    jvmToolchain(11)
}

ktlint {
    version.set("0.43.2")
    debug.set(false)
    verbose.set(true)
    filter {
        exclude("**/generated/**")
        exclude("**/data/PluralData.kt")
        include("**/kotlin/**")
    }
}

//ktlint {
//    attributes {
//        attribute(Bundling.BUNDLING_ATTRIBUTE, getObjects().named(Bundling, Bundling.EXTERNAL))
//    }
//}
