import java.util.Properties

rootProject.name = "buildSrc"

val properties = Properties().apply {
    load(File("${rootProject.projectDir}/../gradle.properties").reader())
}
val kotlinVersion = properties["kotlinVersion"] as String
val springBootVersion = properties["springBootVersion"] as String
val springDependencyManagementVersion = properties["springDependencyManagementVersion"] as String
val liquibaseVersion = properties["liquibaseVersion"] as String

dependencyResolutionManagement {
    versionCatalogs {
        create("buildLibs") {
            version("kotlin", kotlinVersion);
            version("springBoot", springBootVersion);
            version("springDependencyManagement", springDependencyManagementVersion);
            version("liquibase", liquibaseVersion);
            version("ktlint", "11.6.1");

            library("kotlin", "org.jetbrains.kotlin", "kotlin-gradle-plugin").versionRef("kotlin");
            library("kotlinBom", "org.jetbrains.kotlin", "kotlin-bom").versionRef("kotlin");
            library("kotlinAllOpen", "org.jetbrains.kotlin", "kotlin-allopen").versionRef("kotlin");
            library("kotlinJpa", "org.jetbrains.kotlin", "kotlin-noarg").versionRef("kotlin");
            library("kotlinLint", "org.jlleitschuh.gradle", "ktlint-gradle").versionRef("ktlint");
            library("springBoot", "org.springframework.boot", "spring-boot-gradle-plugin").versionRef("springBoot");
            library("springDependencyManagement", "io.spring.gradle", "dependency-management-plugin").versionRef("springDependencyManagement");
            library("liquibase", "org.liquibase", "liquibase-gradle-plugin").versionRef("liquibase");
            library("hibernate", "org.hibernate", "hibernate-gradle-plugin").version("5.6.10.Final");
        }
    }
}
