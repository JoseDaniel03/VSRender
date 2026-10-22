plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.24"
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    mavenCentral()
}

dependencies {
    // Compile against Minecraft 26.3 by default; override via
    // -PvsrenderPaperDevBundle when testing an explicit Paper build.
    paperweight.paperDevBundle(
        providers.gradleProperty("vsrenderPaperDevBundle")
            .orElse("26.3.build.+")
            .get()
    )
    compileOnly(project(":core"))
    compileOnly(project(":bukkit"))
}
