pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "mobile-sdk-universal"

include(":surveysdk")
include(":surveysdk-react-native")

// FIX PATHS - React Native module is in root, not android subfolder
project(":surveysdk-react-native").projectDir = file("surveysdk-react-native")