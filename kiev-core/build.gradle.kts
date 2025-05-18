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
    options.compilerArgs.addAll(arrayOf("-verify","-enable","vnode","-disable","view"))
    kievClasspath = files("${project.rootDir}/symade-core.jar")
}

dependencies {
    implementation(project(":kiev-stdlib"))
}

//tasks.test {
//    useJUnitPlatform()
//}