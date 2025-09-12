pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = providers.gradleProperty("appName").get()
