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
        sourceCompatibility = JavaVersion.VERSION_17  // ← Change to 17
        targetCompatibility = JavaVersion.VERSION_17  // ← Change to 17
    }
    
    kotlinOptions {
        jvmTarget = "17"  // ← Change to 17 to match Java
    }

    lint {
        checkReleaseBuilds = false
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

// ADD THIS - Modern Kotlin toolchain approach
kotlin {
    jvmToolchain(17)  // ← Add this for proper toolchain setup
}

dependencies {
    api("com.facebook.react:react-android:0.72.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
       // IF YOU MADE ANY CHANGES WTIH CORE MODULES, USE PROJECT IMPLEMENTATION FOR JITPACK BUILD AND COMMEND OUT LATER FOR NPM RELEASE
    implementation(project(":surveysdk"))
       // USE TAG NUMBER FOR NPM RELEASE
    //implementation("com.github.mustafadenizer-c4f.mobile-sdk-universal:surveysdk:v1.2.14")
      // WHILE YOU DONT HAVE TAG NUMBER
    //implementation("com.github.mustafadenizer-c4f.mobile-sdk-universal:surveysdk:main-SNAPSHOT")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.12.0")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.mustafadenizer-c4f"
                artifactId = "surveysdk-react-native"
                version = "1.2.13"
                
                pom {
                    name.set("Survey SDK React Native")
                    description.set("React Native bridge for Survey SDK")
                    url.set("https://github.com/mustafadenizer-c4f/mobile-sdk-universal")
                    
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