pluginManagement {
    plugins {
        kotlin("jvm") version "2.1.21"
    }
}


rootProject.name = "pianoroll-sample"
//include(":pianoroll")
//project(":pianoroll").projectDir = File("../pianoroll")
includeBuild("../pianoroll")
