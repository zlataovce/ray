plugins {
    alias(libs.plugins.licenser)
    `java-library`
}

allprojects {
    group = "me.kcra"
    version = "1.0.0-SNAPSHOT"
    description = "block game bytecode transformation"
}

subprojects {
    apply {
        plugin("java-library")
        plugin("org.cadixdev.licenser")
    }

    repositories {
        mavenCentral()
    }

    java.toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }

    license {
        header(rootProject.file("license_header.txt"))
        exclude("**/plugin.yml")
    }
}
