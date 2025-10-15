// build.gradle.kts
plugins {
    id("com.android.library") version "8.1.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("maven-publish") apply false
}

// Configure publishing for all subprojects
subprojects {
    apply(plugin = "maven-publish")
    
    group = "com.github.mustafadenizer-c4f"
    version = "1.0.0"
    
    afterEvaluate {
        publishing {
            publications {
                create<MavenPublication>("release") {
                    from(components["release"])
                    groupId = "com.github.mustafadenizer-c4f.mobile-sdk-universal"
                    artifactId = project.name
                    version = "main-SNAPSHOT"
                    
                    // Add Javadoc and sources if needed
                    // artifact(javadocJar)
                    // artifact(sourcesJar)
                }
            }
        }
    }
}