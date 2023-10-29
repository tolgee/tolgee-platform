package io.tolgee.gradle

import org.apache.tools.ant.taskdefs.condition.Os

plugins {
    idea
    eclipse
}

repositories {
    mavenCentral()
    maven(url = "https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
}

apply(plugin = "java")
apply(plugin = "idea")
apply(plugin = "eclipse")

if (System.getenv().containsKey("VERSION")) {
    project.version = System.getenv().get("VERSION") as String
} else {
    project.version = "local"
}

val npmCommandName by extra(if (Os.isFamily(Os.FAMILY_WINDOWS)) "npm.cmd" else "npm")
