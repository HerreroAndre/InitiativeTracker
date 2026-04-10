plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.dmc.initiativetracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dmc.initiativetracker"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Con Compose BOM, no necesitás setear kotlinCompilerExtensionVersion
    // salvo que tu template lo exija. Si lo exige, lo dejamos como está.
    composeOptions {
        // kotlinCompilerExtensionVersion = "..." // dejalo si ya está
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // --- Compose (BOM) ---
    implementation(platform("androidx.compose:compose-bom:2026.02.00"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.02.00"))

    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.activity:activity-compose")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.compose.material3:material3")

    // --- Navigation Compose ---
    implementation("androidx.navigation:navigation-compose:2.9.5")

    // --- Lifecycle / ViewModel ---
    implementation("androidx.activity:activity-compose:1.12.0")

    // --- Coroutines ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.0")

    // --- Room (con KSP) ---
    implementation("androidx.room:room-runtime:2.8.1")
    implementation("androidx.room:room-ktx:2.8.1")
    ksp("androidx.room:room-compiler:2.8.1")

    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-text")

    implementation("io.coil-kt:coil-compose:2.7.0")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

}