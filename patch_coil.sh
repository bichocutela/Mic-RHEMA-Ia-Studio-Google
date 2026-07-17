sed -i 's/media3 = "1.2.1"/media3 = "1.2.1"\ncoil = "2.6.0"/' gradle/libs.versions.toml
sed -i 's/name = "media3-session", version.ref = "media3" }/name = "media3-session", version.ref = "media3" }\ncoil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }/' gradle/libs.versions.toml
sed -i 's/implementation(libs.androidx.media3.session)/implementation(libs.androidx.media3.session)\n    implementation(libs.coil.compose)/' app/build.gradle.kts
