package io.tolgee.gradle

import gradle.kotlin.dsl.plugins._1bc66e29931c9cc95ac26dbbe0d9f615.org
import org.gradle.kotlin.dsl.kotlin

plugins {
    id("base")
    java
    kotlin("jvm")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.allopen")
    id("org.jlleitschuh.gradle.ktlint")
}

//apply(plugin = "kotlin")
//apply(plugin = "org.jetbrains.kotlin.kapt")
//apply(plugin = "org.jetbrains.kotlin.plugin.allopen")

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

idea {
    module {
        generatedSourceDirs += files("${project.buildDir}/generated/source/kapt/main", "${project.buildDir}/generated/source/kaptKotlin/main")
    }
}
