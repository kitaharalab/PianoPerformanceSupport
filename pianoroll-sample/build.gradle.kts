plugins {
    id("java")
    kotlin("jvm")
    application
}

group = "jp.kthrlab.pianoroll_app"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    // Define the main class for the application.
    mainClass.set("jp.kthrlab.pianorollsample.MyApp")
}


//sourceSets {
//    main {
//        resources {
//            srcDirs("src/main/resources")  // メインのリソースディレクトリ
////            srcDir("$projectDir/resources")
//        }
//    }
//}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"), "exclude" to listOf<String>())))
    implementation("jp.kthrlab:pianoroll")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

task("printEnv") {
//    println(projectDir)
    println(sourceSets["main"].resources.srcDirs)
    println(sourceSets["main"].runtimeClasspath.files)
//    println("sourceSets.main.output.asPath = ${sourceSets["main"].output.asPath}")
}
