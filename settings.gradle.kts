pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

rootProject.name = "symade"
include("kiev-stdlib")
include("kiev-core")
include("kiev-dump")
include("kiev-compiler")
include("symade-fmt")
include("symade-gui")
