plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")

    // Kotlin compiler plugin for Jetpack Compose
    alias(libs.plugins.compose.compiler)

    // Kotlin serialization plugin for type safe routes and navigation arguments
    kotlin("plugin.serialization") version "2.0.21"

    // Safe Args plugin for type-safe navigation arguments
    id("androidx.navigation.safeargs.kotlin") version "2.9.0"
}

android {
    namespace = "com.dex.engrisk"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dex.engrisk"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    kotlinOptions {
        jvmTarget = "17"
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.15.0"))
    // When using the BoM, don't specify versions in Firebase dependencies
    implementation("com.google.firebase:firebase-analytics")
    // Firebase Authentication:
     implementation("com.google.firebase:firebase-auth")
    // Cloud Firestore:
    implementation("com.google.firebase:firebase-firestore")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.1")
    // LiveData
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.1")

    // Fragment KTX (để dùng by viewModels())
    implementation("androidx.fragment:fragment-ktx:1.8.8")

    // Compose
    implementation("androidx.fragment:fragment-compose:1.8.8")

    // Kotlin Preference KTX
    implementation("androidx.preference:preference-ktx:1.2.1")

    // Cardview
    implementation("androidx.cardview:cardview:1.0.0")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // Material Components
    implementation("androidx.compose.material:material:1.8.2")

    // *** Navigation Component ***
    // Navigation Component for Android, used for managing app navigation
    val nav_version = "2.9.0"

    // Jetpack Compose integration
    implementation("androidx.navigation:navigation-compose:$nav_version")

    // Views/Fragments integration
    implementation("androidx.navigation:navigation-fragment:$nav_version")
    implementation("androidx.navigation:navigation-ui:$nav_version")

    // Feature module support for Fragments
    implementation("androidx.navigation:navigation-dynamic-features-fragment:$nav_version")

    // Testing Navigation
    androidTestImplementation("androidx.navigation:navigation-testing:$nav_version")

    // JSON serialization library, works with the Kotlin serialization plugin
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Coroutines for asynchronous programming
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.9")
}