import java.util.Properties

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
}

// Release signing key lives outside the repo. Default: ~/.android/thankyou-meter/keystore.properties
// (storeFile, storePassword, keyAlias, keyPassword); override with THANKYOUMETER_KEYSTORE_PROPERTIES.
// Without it the release build is signed with the debug key so it still installs (not for submission).
val releaseKeyProps = Properties().apply {
  val f = file(System.getenv("THANKYOUMETER_KEYSTORE_PROPERTIES")
    ?: "${System.getProperty("user.home")}/.android/thankyou-meter/keystore.properties")
  if (f.exists()) f.inputStream().use { load(it) }
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
    versionName = "1.0.0"
  }
  signingConfigs {
    if (releaseKeyProps.getProperty("storeFile") != null) {
      create("release") {
        storeFile = file(releaseKeyProps.getProperty("storeFile"))
        storePassword = releaseKeyProps.getProperty("storePassword")
        keyAlias = releaseKeyProps.getProperty("keyAlias")
        keyPassword = releaseKeyProps.getProperty("keyPassword")
      }
    }
  }
  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
    }
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
