pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "mobile-sdk-universal"

// CORRECT PATHS for your structure
include(":surveysdk")
include(":surveysdk-react-native")

// Correct project directory mappings
project(":surveysdk").projectDir = file("surveysdk")
project(":surveysdk-react-native").projectDir = file("surveysdk-react-native/android")