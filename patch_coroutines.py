import re

with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

if 'kotlinx-coroutines-play-services' not in content:
    content = content.replace(
        'implementation(libs.androidx.core.ktx)',
        'implementation(libs.androidx.core.ktx)\n    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")'
    )

with open('app/build.gradle.kts', 'w') as f:
    f.write(content)
