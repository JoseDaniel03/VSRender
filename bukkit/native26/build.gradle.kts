plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.24"
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    mavenCentral()
}

dependencies {
    // Keep the original tested 26.2 bundle as the baseline; allow an explicit
    // 26.3 userdev build through -PvsrenderPaperDevBundle=26.3.build.+.
    paperweight.paperDevBundle(
        providers.gradleProperty("vsrenderPaperDevBundle")
            .orElse("26.2.build.123-stable")
            .get()
    )
    compileOnly(project(":core"))
    compileOnly(project(":bukkit"))
}
