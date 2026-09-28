import java.io.FileInputStream
import java.security.KeyStore
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  // Auto-incrementing versionCode and versionName to enable seamless in-place APK updates
  val versionProps = Properties()
  val versionPropsFile = file("${rootDir}/version.properties")
  if (versionPropsFile.exists()) {
    try {
      FileInputStream(versionPropsFile).use { versionProps.load(it) }
    } catch (_: Exception) {}
  }

  val autoVersionCode: Int = versionProps.getProperty("versionCode")?.toIntOrNull()
    ?: (((System.currentTimeMillis() / 1000) - 1700000000L) / 10).toInt()

  val autoVersionName: String = versionProps.getProperty("versionName")
    ?: "1.0.$autoVersionCode"

  defaultConfig {
    applicationId = "com.aistudio.fluxlauncher.vknwre"
    minSdk = 24
    targetSdk = 36
    versionCode = autoVersionCode
    versionName = autoVersionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    androidResources.localeFilters += listOf("en")
  }

  signingConfigs {
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
    create("release") {
      val envKeystore = System.getenv("KEYSTORE_PATH")
      val keystorePath = envKeystore ?: "${rootDir}/my-upload-key.jks"
      val keystoreFile = file(keystorePath)
      val storePass = System.getenv("STORE_PASSWORD") ?: System.getenv("KEYSTORE_PASSWORD") ?: System.getenv("KEYSTORE_PASS")
      val keyPass = System.getenv("KEY_PASSWORD") ?: System.getenv("KEY_PASS") ?: storePass
      val alias = System.getenv("KEY_ALIAS") ?: "upload"

      var isValidKeystore = false
      if (keystoreFile.exists() && keystoreFile.length() > 0 && !storePass.isNullOrBlank()) {
        try {
          val ks = KeyStore.getInstance(KeyStore.getDefaultType())
          FileInputStream(keystoreFile).use { stream ->
            ks.load(stream, storePass.toCharArray())
          }
          if (ks.containsAlias(alias) && ks.getKey(alias, (keyPass ?: storePass).toCharArray()) != null) {
            isValidKeystore = true
          }
        } catch (_: Exception) {
          isValidKeystore = false
        }
      }

      if (isValidKeystore) {
        storeFile = keystoreFile
        storePassword = storePass
        keyAlias = alias
        keyPassword = keyPass
      } else {
        // Fallback to debug keystore so release build compiles and packages successfully
        storeFile = file("${rootDir}/debug.keystore")
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = true
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      isCrunchPngs = true
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  packaging {
    resources {
      excludes += listOf(
        "/META-INF/{AL2.0,LGPL2.1}",
        "/META-INF/DEPENDENCIES",
        "/META-INF/LICENSE",
        "/META-INF/LICENSE.txt",
        "/META-INF/license.txt",
        "/META-INF/NOTICE",
        "/META-INF/NOTICE.txt",
        "/META-INF/notice.txt",
        "/META-INF/ASL2.0",
        "/META-INF/*.kotlin_module",
        "/META-INF/*.version",
        "DebugProbesKt.bin",
        "okhttp3/internal/publicsuffix/publicsuffixes.gz",
        "**/*.kotlin_builtins"
      )
    }
  }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  // implementation(libs.androidx.room.ktx)
  // implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  // implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  // implementation(libs.firebase.appcheck.recaptcha)
  // implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // implementation(libs.logging.interceptor)
  // implementation(libs.moshi.kotlin)
  // implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  // implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  // debugImplementation(libs.androidx.compose.ui.test.manifest)
  // debugImplementation(libs.androidx.compose.ui.tooling)
  // "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)
}
