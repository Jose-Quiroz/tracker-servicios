import java.util.Properties

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        load(file.inputStream())
    }
}
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.upn.trackerservicios"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.upn.trackerservicios"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        // --- AQUÍ INYECTAMOS EL TOKEN PÚBLICO DE MAPBOX ---
        val mapboxToken = localProps.getProperty("MAPBOX_PUBLIC_TOKEN") ?: ""
        resValue("string", "mapbox_access_token", "\"$mapboxToken\"")

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
    buildFeatures {
        compose = true
        resValues = true
    }
}

dependencies {
    implementation("androidx.compose.material:material-icons-extended")
    // --- LIBRERÍAS DE MAPBOX (Reemplazaron a las de Google Maps) ---
    implementation("com.mapbox.maps:android:11.9.0")
    implementation("com.mapbox.extension:maps-compose:11.9.0")

    // --- TUS LIBRERÍAS INTACTAS ---
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}