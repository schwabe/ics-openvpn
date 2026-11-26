plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.parafino.vpn"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.parafino.vpn"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        missingDimensionStrategy("implementation", "skeleton")
        missingDimensionStrategy("ovpnimpl", "ovpn23")
    }

    buildFeatures { compose = true }

    packaging { jniLibs { useLegacyPackaging = true } }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":main"))
    implementation(platform("androidx.compose:compose-bom:2025.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
}
