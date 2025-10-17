plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.example.surveysdk.reactnative"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        targetSdk = 34
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
    }

    // FIX: Disable annotations processing for React Native
    lint {
        checkReleaseBuilds = false
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    // FIX: Use compileOnly for React Native - it will be provided by the React Native app
    compileOnly("com.facebook.react:react-android:0.72.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
    implementation(project(":surveysdk"))
    
    // Add AndroidX dependencies that React Native uses
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.12.0")
}

// FIX: Simplified publishing without React Native dependency in POM
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.mustafadenizer-c4f"
                artifactId = "surveysdk-react-native"
                version = "1.0.0"
                
                pom {
                    name.set("Survey SDK React Native")
                    description.set("React Native bridge for Survey SDK")
                    url.set("https://github.com/mustafadenizer-c4f/mobile-sdk-universal")
                    
                    // Remove React Native from dependencies in POM
                    // It will be provided by the React Native app environment
                    
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    
                    developers {
                        developer {
                            id.set("mustafadenizer-c4f")
                            name.set("Mustafa Denizer")
                        }
                    }
                }
            }
        }
    }
}