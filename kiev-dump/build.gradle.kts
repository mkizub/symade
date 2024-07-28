plugins {
    id("java")
}

group = "org.symade.kiev"
version = "0.6.0-SNAPSHOT"

repositories {
    mavenCentral()
}

sourceSets {
    main {
        java {
            srcDir("src/main/java")
        }
    }
}

dependencies {
//    testImplementation(platform("org.junit:junit-bom:5.9.1"))
//    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(files("${project.rootDir}/symade-core.jar", "${project.rootDir}/bin/xpp3-1.1.4c.jar"))
    implementation(project(":kiev-stdlib"))
    implementation(project(":kiev-core"))
}

//tasks.test {
//    useJUnitPlatform()
//}