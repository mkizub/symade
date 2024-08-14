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
    // remove -enable vnode -enable view
    // kiev.vtree.CompilerException: Auto-generated field with name $node_type_info is not found
    options.compilerArgs.addAll(arrayOf("-verify","-disable","vnode","-disable","view","-v","-debug","on"))
}

dependencies {
//    testImplementation(platform("org.junit:junit-bom:5.9.1"))
//    testImplementation("org.junit.jupiter:junit-jupiter")
    // TODO needed for rewrite templates, should not be needed
    compileOnly(files("${project.rootDir}/symade-core.jar"))
}

//tasks.test {
//    useJUnitPlatform()
//}