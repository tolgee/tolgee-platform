package io.tolgee.gradle

import org.gradle.kotlin.dsl.kotlin

plugins {
    java
    kotlin("jvm")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.allopen")
    id("org.jlleitschuh.gradle.ktlint")
}

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

//task ktlint(type: JavaExec, group: "verification") {
//    description = "Check Kotlin code style."
//    classpath = buildLibs.kotlinLint
//    mainClass = "com.pinterest.ktlint.Main"
//    args "**/*.kt", "!**/data/PluralData.kt", '../billing/**/*.kt'
//}
//
//task ktlintFormat(type: JavaExec, group: "formatting") {
//    description = "Fix Kotlin code style deviations."
//    classpath = buildLibs.kotlinLint
//    mainClass = "com.pinterest.ktlint.Main"
//    args "-F", "**/*.kt", "!**/data/PluralData.kt", '../billing/**/*.kt'
//    jvmArgs = ["--add-opens=java.base/java.lang=ALL-UNNAMED"]
//}
