plugins {
    kotlin("jvm") version "2.0.21"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    sourceSets["test"].kotlin.srcDir("tests")
}

application {
    mainClass.set("depgraph.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
