pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
    }
}

rootProject.name = "symade"
include("kiev-stdlib")
include("kiev-core")
include("kiev-compiler")
include("kiev-dump")
include("symade-fmt")
include("symade-gui")
