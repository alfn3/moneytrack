import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.ksp)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  id("kotlin-parcelize")
}

android {
    namespace = "com.example.cashmanage"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.cashmanage"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }
        val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY") ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
        excludes += "/META-INF/DEPENDENCIES"
      }
    }
}

kotlin {
    jvmToolchain(17)
}



dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)
  
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation("androidx.compose.material:material-icons-extended:1.6.0")
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  // Instrumented tests
  // Removed ui-test dependencies for now to simplify build

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  // Removed test dependencies for simplicity

  // Navigation
  // Navigation
  implementation(libs.androidx.navigation.compose)

  // Room
  val room_version = "2.6.1"
  implementation("androidx.room:room-runtime:$room_version")
  implementation("androidx.room:room-ktx:$room_version")
  ksp("androidx.room:room-compiler:$room_version")

  // CameraX
  val camerax_version = "1.3.4"
  implementation("androidx.camera:camera-core:${camerax_version}")
  implementation("androidx.camera:camera-camera2:${camerax_version}")
  implementation("androidx.camera:camera-lifecycle:${camerax_version}")
  implementation("androidx.camera:camera-view:${camerax_version}")

  // Google ML Kit Text Recognition
  implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")
  implementation(libs.kotlinx.coroutines.play.services)

  // Gemini API
  implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

  // WorkManager
  implementation("androidx.work:work-runtime-ktx:2.9.0")

  // MPAndroidChart
  implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

  // Coil
  implementation("io.coil-kt:coil-compose:2.6.0")

  // Retrofit & Gson
  implementation("com.squareup.retrofit2:retrofit:2.11.0")
  implementation("com.squareup.retrofit2:converter-gson:2.11.0")
  
  // Credential Manager API
  implementation("androidx.credentials:credentials:1.3.0")
  implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
  implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

  // Google Sign-In and API Client
  implementation("com.google.android.gms:play-services-auth:21.0.0")
  implementation("com.google.api-client:google-api-client-android:1.33.0") {
      exclude(group = "org.apache.httpcomponents")
  }
  implementation("com.google.apis:google-api-services-sheets:v4-rev20210629-1.32.1") {
      exclude(group = "org.apache.httpcomponents")
  }
  implementation("com.google.apis:google-api-services-drive:v3-rev20211107-1.32.1") {
      exclude(group = "org.apache.httpcomponents")
  }
  implementation("com.google.http-client:google-http-client-gson:1.41.0") {
      exclude(group = "org.apache.httpcomponents")
  }
  implementation("com.google.guava:guava:31.1-android")
}
