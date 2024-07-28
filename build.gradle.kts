plugins {
    id("java")
    id("idea")
}

group = "org.symade.kiev"
version = "0.6.0-SNAPSHOT"

repositories {
    mavenCentral()
}

//sourceSets {
//    main {
//        kiev {
//            srcDir("src")
//                    .include("kiev/**/*.java")
//                    .include("kiev/**/*.xml")
//                    .exclude("kiev/gui/**/*")
//                    .exclude("*.*")
//            srcDir("stx-fmt")
//                    .include("*.xml")
//        }
//
//        java {
//            srcDir("src")
//                    .include("kiev/gui/**/*.java")
//        }
//    }
//}
//
//fun KievCompile.setup(stage: Int) {
//    source = sourceSets["main"].kiev
//    destinationDirectory = layout.buildDirectory.dir("kclasses$stage")
//    options.compilerArgs.addAll(arrayOf("-verify","-enable","vnode","-enable","view"))
//    if (stage == 1) {
//        classpath = files("bin/symade-06.jar")
//        kievClasspath = files("bin/symade-06.jar")
//    } else {
//        var p = layout.buildDirectory;
//        classpath = files(p.dir("kclasses${stage - 1}"), p.dir("jclasses${stage - 1}"))
//        kievClasspath = files(p.dir("kclasses${stage - 1}"))
//    }
//    if (stage > 1)
//        dependsOn(tasks["symadeStage_${stage-1}"])
//}
//
//fun JavaCompile.setup(stage: Int) {
//    source(sourceSets["main"].java)
//    destinationDirectory = layout.buildDirectory.dir("jclasses$stage")
//    classpath = files(layout.buildDirectory.dir("kclasses$stage"), "bin/swt-win.jar")
//    dependsOn(tasks["compileKiev_$stage"])
//    options.isDeprecation = true
//}
//
//fun setupStage(stage: Int) {
//    val k = tasks.register<KievCompile>("compileKiev_$stage") {
//        setup(stage)
//    }
//    val j = tasks.register<JavaCompile>("compileJava_$stage") {
//        setup(stage)
//    }
//    tasks.register("symadeStage_$stage") {
//        group = "build"
//        dependsOn(k)
//        dependsOn(j)
//    }
//}
//
//setupStage(1)
//setupStage(2)
//setupStage(3)
//
//tasks.compileKiev {
//    options.compilerArgs.addAll(arrayOf("-verify","-enable","vnode","-enable","view"))
//    kievClasspath = files("bin/symade-06.jar")
//}
//
//tasks.compileJava {
//    dependsOn(tasks.compileKiev)
//}

dependencies {
//    testImplementation(platform("org.junit:junit-bom:5.9.1"))
//    testImplementation("org.junit.jupiter:junit-jupiter")
//    implementation(project(":kiev-stdlib"))
//    implementation(project(":kiev-core"))
//    implementation(project(":kiev-dump"))
//    implementation(project(":kiev-compiler"))
//    implementation(project(":symade-fmt"))
//    implementation(project(":symade-gui"))
//    implementation(files("src/symade-core.jar", "bin/swt-win.jar"))
}

//tasks.test {
//    useJUnitPlatform()
//}