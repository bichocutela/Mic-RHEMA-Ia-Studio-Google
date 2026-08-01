plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.secrets)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

import java.util.Properties
import java.io.FileInputStream
import java.io.FileOutputStream

val versionFile = rootProject.file("version.properties")
val versionProps = Properties()

if (versionFile.exists()) {
    versionProps.load(FileInputStream(versionFile))
} else {
    versionProps["MAJOR"] = "2"
    versionProps["MINOR"] = "0"
    versionProps["PATCH"] = "1"
    versionProps["BUILD"] = "0"
}

val isBuilding = gradle.startParameter.taskNames.any { it.contains("assemble") || it.contains("build") || it.contains("bundle") }

var buildNum = versionProps["BUILD"].toString().toInt()
if (isBuilding) {
    buildNum++
    versionProps["BUILD"] = buildNum.toString()
    versionProps.store(FileOutputStream(versionFile), "Auto-incremented build number")
}

val major = versionProps["MAJOR"].toString().toInt()
val minor = versionProps["MINOR"].toString().toInt()
val patch = versionProps["PATCH"].toString().toInt()

val appVersionCode = major * 1000000 + minor * 10000 + patch * 100 + buildNum
val appVersionName = "${major}.${minor}.${patch}.${buildNum}"

android {
    namespace = "com.aistudio.micrhema"

    lint {
        disable += "UnsafeOptInUsageError"
    }

    compileSdk = 35

    defaultConfig {
        applicationId = "com.aistudio.micrhema.xqpq"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        vectorDrawables {
            useSupportLibrary = true
        }
                                buildConfigField("String", "GEMINI_API_KEY", "\"\"")
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            } else if (System.getenv("KEYSTORE_FILE") != null) {
                storeFile = file(System.getenv("KEYSTORE_FILE") as String)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists() || System.getenv("KEYSTORE_FILE") != null) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
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
    implementation(libs.firebase.config)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)
    implementation(libs.firebase.crashlytics)
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
    (it.name.contains("GoogleServices") || it.name == "preBuild") && 
    it.name != "ensureGoogleServicesJson" 
}.configureEach {
    dependsOn(ensureGoogleServicesJson)
}


tasks.configureEach {
    if (name == "assembleRelease" || name == "bundleRelease") {
        doLast {
            try {
                val tagProcess = ProcessBuilder("git", "tag", "-a", "v${appVersionName}", "-m", "Release v${appVersionName}")
                    .directory(rootProject.rootDir)
                    .redirectErrorStream(true)
                    .start()
                tagProcess.waitFor()
                val tagOutput = tagProcess.inputStream.bufferedReader().readText()
                if (tagProcess.exitValue() == 0) {
                    println("Successfully created git tag v${appVersionName}")
                } else {
                    println("Failed to create git tag: $tagOutput")
                }
            } catch (e: Exception) {
                println("Exception while creating git tag: ${e.message}")
            }
        }
    }
}
