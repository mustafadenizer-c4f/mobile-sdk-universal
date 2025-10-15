// react-native/surveysdk-react-native/android/build.gradle.kts
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
}

dependencies {
    implementation(project(":surveysdk"))
    
    // React Native
    implementation("com.facebook.react:react-android:+")
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
    implementation("androidx.core:core-ktx:1.12.0")
}

// Publishing configuration
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.mustafadenizer-c4f.mobile-sdk-universal"
                artifactId = "surveysdk-react-native"
                version = "main-SNAPSHOT"
                
                pom {
                    name.set("Survey SDK React Native")
                    description.set("React Native bridge for Survey SDK")
                    url.set("https://github.com/mustafadenizer-c4f/mobile-sdk-universal")
                }
            }
        }
    }
}