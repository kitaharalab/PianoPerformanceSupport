plugins {
    id("java")
    kotlin("jvm")
}

group = "jp.kthrlab"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"), "exclude" to listOf<String>())))
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.apache.pdfbox:pdfbox:2.0.30")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}