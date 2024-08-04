plugins {
    id("java")
}

group = "org.symade.kiev"
version = "0.6.0-SNAPSHOT"

sourceSets {
    main {
        java {
            srcDir("src/main/java")
        }
    }
}

tasks.compileJava {
    options.isDeprecation = true
    //options.isDebug = true
}

dependencies {
//    testImplementation(platform("org.junit:junit-bom:5.9.1"))
//    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(files("${project.rootDir}/symade-core.jar"))
    implementation(project(":kiev-stdlib"))
    implementation(project(":kiev-core"))
}

//tasks.test {
//    useJUnitPlatform()
//}