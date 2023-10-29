plugins {
    java
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
//    maven { url 'https://repo.spring.io/release' }
}

dependencies {
    implementation(buildLibs.kotlin)
    implementation(buildLibs.kotlinAllOpen)
    implementation(buildLibs.kotlinLint)
    implementation(buildLibs.kotlinJpa)
    implementation(buildLibs.springBoot)
    implementation(buildLibs.springDependencyManagement)
    implementation(buildLibs.liquibase)
    implementation(buildLibs.hibernate)

    // small hack from https://github.com/gradle/gradle/issues/15383#issuecomment-779893192 to access these catalogs in precompiled script plugins
    // implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(files(buildLibs.javaClass.superclass.protectionDomain.codeSource.location))
}
