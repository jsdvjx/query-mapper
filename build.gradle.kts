import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.21"
    application
}

group = "st.wing"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

    implementation("javax.inject:javax.inject:1")
    implementation("io.r2dbc:r2dbc-spi:0.8.3.RELEASE")
    implementation("io.projectreactor:reactor-core:3.2.3.RELEASE")
    implementation("com.google.guava:guava:30.1-jre")
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClassName = "MainKt"
}
tasks.register<Copy>("distJar") {
    dependsOn("build")
    from(project.buildDir.resolve("libs"))
    into("C:\\Users\\JinXing\\project\\metal-policy\\libs")
}
