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
    implementation(project(":kiev-stdlib"))
    implementation(project(":kiev-core"))
    implementation(project(":kiev-dump"))
    implementation(project(":kiev-compiler"))
    implementation(project(":symade-fmt"))
    compileOnly(files("${project.rootDir}/bin/swt-win.jar"))
}

//tasks.test {
//    useJUnitPlatform()
//}