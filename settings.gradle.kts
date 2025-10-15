// settings.gradle.kts
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

// Include React Native bridge as a submodule
include(":surveysdk-react-native")
project(":surveysdk-react-native").projectDir = file("../react-native/surveysdk-react-native/android")