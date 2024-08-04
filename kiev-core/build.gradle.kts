plugins {
    id("java")
    id("kiev-gradle-plugin") version "0.6.0-SNAPSHOT"
}

group = "org.symade.kiev"
version = "0.6.0-SNAPSHOT"

repositories {
    mavenCentral()
}

sourceSets {
    main {
        kiev {
            srcDir("src/main")
        }
    }
}

tasks.compileKiev {
    options.compilerArgs.addAll(arrayOf("-verify","-enable","vnode","-enable","view"))
//    kievClasspath = files("${project.rootDir}/symade-core.jar")
}

dependencies {
    implementation(project(":kiev-stdlib"))
    implementation(files("${project.rootDir}/symade-core.jar"))
//    testImplementation(platform("org.junit:junit-bom:5.9.1"))
//    testImplementation("org.junit.jupiter:junit-jupiter")
}

//tasks.test {
//    useJUnitPlatform()
//}