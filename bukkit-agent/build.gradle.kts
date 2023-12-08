plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.asm)
}

tasks.withType<Jar> {
    manifest {
        attributes["Premain-Class"] = "me.kcra.ray.bukkit.agent.RayAgent"
    }
}
