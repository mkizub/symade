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
    options.compilerArgs.addAll(arrayOf("-verify","-enable","vnode","-enable","view"))
    classpath = files("${project.rootDir}/symade-core.jar")
    kievClasspath = files("${project.rootDir}/symade-core.jar")
}

dependencies {
//    testImplementation(platform("org.junit:junit-bom:5.9.1"))
//    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(project(":kiev-stdlib"))
    implementation(project(":kiev-core"))
    implementation(project(":kiev-dump"))
}

//tasks.test {
//    useJUnitPlatform()
//}