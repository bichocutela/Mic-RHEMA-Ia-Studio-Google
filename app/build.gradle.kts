plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.secrets)
}

android {
    namespace = "com.aistudio.micrhema"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aistudio.micrhema.xqpq"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"\"")
        buildConfigField("String", "FIREBASE_APP_ID", "\"\"")
        buildConfigField("String", "FIREBASE_API_KEY", "\"\"")
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
    
    // Explicitly set debug signing config to use rootDir debug.keystore
    signingConfigs {
        getByName("debug") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.core.ktx)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.material.icons.extended)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.coil.compose)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)

    debugImplementation(libs.androidx.ui.tooling)
}

secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
}
