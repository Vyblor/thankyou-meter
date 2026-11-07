plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
}

android {
  namespace = "app.thankyoumeter"
  compileSdk = 34
  useLibrary("android.car")

  defaultConfig {
    applicationId = "app.thankyoumeter"
    minSdk = 28
    targetSdk = 34
    versionCode = 1
    versionName = "0.1.0"
  }
  buildTypes {
    release { isMinifyEnabled = false }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions { jvmTarget = "17" }
  buildFeatures { compose = true; buildConfig = true }
  composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
}

dependencies {
  implementation("ai.pleos.playground:Vehicle:2.3.2")
  implementation("ai.pleos.playground:SpeechToText:3.3.1")
  implementation("ai.pleos.playground:TextToSpeech:3.3.1")

  val bom = platform("androidx.compose:compose-bom:2024.06.00")
  implementation(bom)
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.foundation:foundation")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.material:material-icons-extended")
  implementation("androidx.activity:activity-compose:1.9.0")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
  implementation("com.google.zxing:core:3.5.3")

  testImplementation("junit:junit:4.13.2")
}
