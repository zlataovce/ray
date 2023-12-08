plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

fun includePrefixed(vararg names: String) {
    names.forEach { name ->
        include(":ray-$name")
        project(":ray-$name").projectDir = file(name)
    }
}

fun includeComposite(name: String, vararg modules: String) {
    modules.forEach { module ->
        include(":ray-$name-$module")
        project(":ray-$name-$module").projectDir = file("$name/$module")
    }
}

rootProject.name = "ray"

includePrefixed("api", "common", "bukkit", "bukkit-agent")
includeComposite("transform", "reobf")
