plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.jijipos"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.jijipos"
        minSdk = 27
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")

    annotationProcessor("androidx.room:room-compiler:$roomVersion")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}