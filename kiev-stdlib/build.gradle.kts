plugins {
    id("java")
    id("kiev-gradle-plugin") version "0.6.0-SNAPSHOT"
}

group = "org.symade.kiev"
version = "0.6.0-SNAPSHOT"

sourceSets {
    main {
        kiev {
            srcDir("src/main")
        }
    }
}

tasks.compileKiev {
    kievClasspath = files("${project.rootDir}/symade-core.jar")
    options.compilerArgs.addAll(arrayOf("-verify","-disable","vnode","-disable","view","-disable","logic"))
}

dependencies {
}

//tasks.test {
//    useJUnitPlatform()
//}