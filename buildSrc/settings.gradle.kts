rootProject.name = "buildSrc"

dependencyResolutionManagement {
    versionCatalogs {
        create("buildLibs") {
            // version("kotlin", String.valueOf(kotlinVersion));
            version("kotlin", "1.9.10");
            version("ktlint", "11.6.1");
            version("springBoot", "2.7.13");
            version("springDependencyManagement", "1.0.11.RELEASE");
            version("liquibase", "2.1.1");

            library("kotlin", "org.jetbrains.kotlin", "kotlin-gradle-plugin").versionRef("kotlin");
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
