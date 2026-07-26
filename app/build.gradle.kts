plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.secrets)
    alias(libs.plugins.google.services)
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
                                buildConfigField("String", "GEMINI_API_KEY", "\"\"")
    }

    signingConfigs {
        val existingKeystore = listOf(
            file("${rootDir}/release.keystore"),
            file("${rootDir}/app/release.keystore"),
            file("release.keystore"),
            file("app/release.keystore"),
            file("${rootDir}/debug.keystore"),
            file("${rootDir}/app/debug.keystore"),
            file("debug.keystore"),
            file("applet/debug.keystore"),
            file("${rootDir}/applet/debug.keystore")
        ).firstOrNull { it.exists() } ?: file("${rootDir}/debug.keystore")

        getByName("debug") {
            storeFile = existingKeystore
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "android"
            keyAlias = System.getenv("KEYSTORE_ALIAS") ?: if (existingKeystore.name.contains("release")) "release" else "androiddebugkey"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "android"
        }

        create("release") {
            storeFile = existingKeystore
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "android"
            keyAlias = System.getenv("KEYSTORE_ALIAS") ?: if (existingKeystore.name.contains("release")) "release" else "androiddebugkey"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    implementation(libs.firebase.auth)
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

// Ensure google-services.json and keystore are present in all possible search paths before build tasks run
val ensureKeystoreExists = tasks.register("ensureKeystoreExists") {
    doLast {
        val existingKeystore = listOf(
            file("${rootDir}/release.keystore"),
            file("${rootDir}/app/release.keystore"),
            file("release.keystore"),
            file("app/release.keystore"),
            file("${rootDir}/debug.keystore"),
            file("${rootDir}/app/debug.keystore"),
            file("debug.keystore"),
            file("applet/debug.keystore"),
            file("${rootDir}/applet/debug.keystore")
        ).firstOrNull { it.exists() }

        if (existingKeystore == null || !existingKeystore.exists()) {
            val target = file("${rootDir}/debug.keystore")
            try {
                target.parentFile?.mkdirs()
                val pb = ProcessBuilder(
                    "keytool", "-genkeypair",
                    "-alias", "androiddebugkey",
                    "-keyalg", "RSA",
                    "-keysize", "2048",
                    "-validity", "10000",
                    "-keypass", "android",
                    "-keystore", target.absolutePath,
                    "-storepass", "android",
                    "-dname", "CN=Android Debug, O=Android, C=US"
                )
                pb.start().waitFor()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

val ensureGoogleServicesJson = tasks.register("ensureGoogleServicesJson") {
    doLast {
        val googleServicesContent = """
        {
          "project_info": {
            "project_number": "894363387794",
            "project_id": "mic-rhema",
            "storage_bucket": "mic-rhema.firebasestorage.app"
          },
          "client": [
            {
              "client_info": {
                "mobilesdk_app_id": "1:894363387794:android:bb0b58cf5a668a7685234b",
                "android_client_info": {
                  "package_name": "com.aistudio.micrhema.xqpq"
                }
              },
              "oauth_client": [],
              "api_key": [
                {
                  "current_key": "AIzaSyCHJ9c8AmfjEXMjNTr618kHySkSdjSjWgE"
                }
              ],
              "services": {
                "appinvite_service": {
                  "other_platform_oauth_client": []
                }
              }
            }
          ],
          "configuration_version": "1"
        }
        """.trimIndent()

        val possiblePaths = listOf(
            file("google-services.json"),
            file("app/google-services.json"),
            file("src/google-services.json"),
            file("src/main/google-services.json"),
            file("src/release/google-services.json"),
            file("../google-services.json"),
            file("../app/google-services.json")
        )

        possiblePaths.forEach { targetFile ->
            if (!targetFile.exists()) {
                targetFile.parentFile?.mkdirs()
                targetFile.writeText(googleServicesContent)
            }
        }
    }
}

tasks.matching { 
    (it.name.contains("GoogleServices") || it.name.contains("Signing") || it.name == "preBuild") && 
    it.name != "ensureGoogleServicesJson" && 
    it.name != "ensureKeystoreExists" 
}.configureEach {
    dependsOn(ensureGoogleServicesJson, ensureKeystoreExists)
}

