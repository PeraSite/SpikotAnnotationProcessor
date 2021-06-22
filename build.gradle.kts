plugins {
    kotlin("jvm") version "1.5.10"
    kotlin("kapt") version "1.5.10"
    idea
    `maven-publish`
}

group = "kr.heartpattern"
version = "5.0.0-SNAPSHOT"

repositories {
    maven("https://repo.heartpattern.io/repository/maven-public/")
    maven("https://jitpack.io")
}


val kotlin_version = "1.5.10"

dependencies {
    api("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    api("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    api("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    
    api("com.google.auto.service:auto-service:1.0-rc6")
    kapt("com.google.auto.service:auto-service:1.0-rc6")
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            artifactId = "SpikotAnnotationProcessor"
            artifact(sourcesJar)
            from(project.components["java"])
        }
    }
}
