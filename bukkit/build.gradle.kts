import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    alias(libs.plugins.shadow)
    alias(libs.plugins.plugin.yml)
    alias(libs.plugins.run.paper)
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.asm) // reinstate the excluded dependency
    implementation(project(":ray-common")) {
        exclude(module = "asm") // bundled with paper
    }
}

bukkit {
    name = "ray"
    website = "https://github.com/zlataovce/ray"
    author = "zlataovce"
    main = "me.kcra.ray.bukkit.plugin.RayPlugin"
    foliaSupported = true
    apiVersion = "1.13"
    load = BukkitPluginDescription.PluginLoadOrder.STARTUP
}

tasks.runServer {
    minecraftVersion("1.20.2")
}
