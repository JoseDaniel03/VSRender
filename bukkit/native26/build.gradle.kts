plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.24"
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    mavenCentral()
}

dependencies {
    paperweight.paperDevBundle("26.2.build.123-stable")
    compileOnly(project(":core"))
    compileOnly(project(":bukkit"))
}
