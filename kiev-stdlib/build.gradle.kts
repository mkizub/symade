plugins {
    id("java")
    id("kiev-gradle-plugin") version "0.6.0-SNAPSHOT"
}

group = "org.symade.kiev"
version = "0.6.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

sourceSets {
    main {
        kiev {
            srcDir("src/main")
        }
    }
}

tasks.compileKiev {
    classpath = files("${project.rootDir}/symade-core.jar", "${project.rootDir}/bin/xpp3-1.1.4c.jar")
    kievClasspath = files("${project.rootDir}/symade-core.jar")
    options.compilerArgs.addAll(arrayOf("-verify","-enable","vnode","-enable","view"))
}

dependencies {
//    testImplementation(platform("org.junit:junit-bom:5.9.1"))
//    testImplementation("org.junit.jupiter:junit-jupiter")
}

//tasks.test {
//    useJUnitPlatform()
//}