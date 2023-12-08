plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(libs.jetbrains.annotations)
    compileOnly(project(":ray-api"))
    implementation(libs.mapping.io)
}

tasks.shadowJar {
    relocate("net.fabricmc.mappingio", "me.kcra.ray.lib.mappingio")
}
