rootProject.name = "vsrender"
include(
    ":core",
    ":core:adventure",
    ":extensions:vscontenido-mechanics",
    ":common-files",
    ":bukkit",
    ":bukkit:legacy",
    ":bukkit:compatibility",
    ":bukkit:compatibility:legacy",
    ":bukkit:loader",
    ":bukkit:proxy",
    ":bukkit:paper-loader",
    ":bukkit:native26"
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://maven.fabricmc.net/")
    }
}