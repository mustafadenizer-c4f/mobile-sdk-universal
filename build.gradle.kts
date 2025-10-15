// build.gradle.kts
plugins {
    id("com.android.library") version "8.1.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("maven-publish") apply false
}

subprojects {
    apply(plugin = "maven-publish")
    
    group = "com.github.mustafadenizer-c4f"
    version = "1.0.0"
}