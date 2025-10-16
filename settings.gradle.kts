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

// CORRECT PATHS based on your structure:
include(":surveysdk")  // This is in root/surveysdk/
include(":surveysdk-react-native")  // This is in root/surveysdk-react-native/

// NO projectDir overrides needed since they're in root