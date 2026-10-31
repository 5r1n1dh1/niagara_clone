import java.io.FileInputStream
import java.security.KeyStore
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.hilt.android)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.kotlinx.serialization)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  // Auto-incrementing versionCode and versionName to enable seamless in-place APK updates.
  //
  // `versionCode` MUST strictly increase for every shipped build, otherwise Android's
  // PackageInstaller refuses an in-place update on the device. When `versionCode` is
  // commented out in version.properties (the default, "auto" mode) we derive a
  // timestamp-based code that grows on every build.
  //
  // `coerceAtLeast(MIN_VERSION_CODE)` guarantees the auto value is ALWAYS higher than any
  // build shipped so far — in particular the pinned `versionCode=2` published in v1.0.1 —
  // so the very first auto build updates in place instead of being rejected.
  val MIN_VERSION_CODE = 2

  val versionProps = Properties()
  val versionPropsFile = file("${rootDir}/version.properties")
  if (versionPropsFile.exists()) {
    try {
      FileInputStream(versionPropsFile).use { versionProps.load(it) }
    } catch (_: Exception) {}
  }

  val autoVersionCode: Int = versionProps.getProperty("versionCode")?.toIntOrNull()
    ?: (((System.currentTimeMillis() / 1000) - 1700000000L) / 10).toInt()
      .coerceAtLeast(MIN_VERSION_CODE)

  val autoVersionName: String = versionProps.getProperty("versionName")
    ?: "1.0.$autoVersionCode"

  println("RESOLVED versionCode=${autoVersionCode} versionName=${autoVersionName}")

  defaultConfig {
    applicationId = "com.aistudio.fluxlauncher.vknwre"
    minSdk = 35
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
    // Persistent release signing key.
    //
    // Keystore resolution order:
    //   1. `KEYSTORE_PATH` env — CI decodes the base64 secret into a file and exports this
    //   2. `${rootDir}/release-key.jks` — the persistent, reproducible release key (gitignored)
    //   3. `${rootDir}/my-upload-key.jks` — legacy location, kept so older setups keep working
    //
    // Passwords/alias come from env vars and fall back to the defaults baked into the
    // project's own `release-key.jks` (see docs/RELEASE.md). Signing EVERY release with
    // the SAME key is what makes in-place updates work on a real device.
    create("release") {
      val storePass: String = System.getenv("STORE_PASSWORD")
        ?: System.getenv("KEYSTORE_PASSWORD")
        ?: System.getenv("KEYSTORE_PASS")
        ?: "fluxstorepass"
      val keyPass: String = System.getenv("KEY_PASSWORD")
        ?: System.getenv("KEY_PASS")
        ?: "fluxkeypass"
      val alias: String = System.getenv("KEY_ALIAS")?.takeIf { it.isNotBlank() } ?: "flux-release"

      val keystoreFile: File? = listOfNotNull(
        System.getenv("KEYSTORE_PATH")?.takeIf { it.isNotBlank() },
        "${rootDir}/release-key.jks",
        "${rootDir}/my-upload-key.jks",
      )
        .map { file(it) }
        .firstOrNull { candidate ->
          candidate.exists() && candidate.length() > 0 && runCatching {
            val ks = KeyStore.getInstance(KeyStore.getDefaultType())
            FileInputStream(candidate).use { stream ->
              ks.load(stream, storePass.toCharArray())
            }
            ks.containsAlias(alias) && ks.getKey(alias, keyPass.toCharArray()) != null
          }.getOrDefault(false)
        }

      if (keystoreFile != null) {
        storeFile = keystoreFile
        storePassword = storePass
        keyAlias = alias
        keyPassword = keyPass
        println("RELEASE SIGNING: using persistent keystore '${keystoreFile.absolutePath}' (alias '$alias')")
      } else {
        // Fallback to debug keystore so release build compiles and packages successfully.
        // This is intentionally loud: a debug-signed APK cannot be updated in place by a
        // later release-signed build (signature mismatch), so CI/local runs in this state
        // produce an APK that must be uninstalled before installing the real release.
        println(
          "RELEASE SIGNING WARNING: no valid release keystore found " +
            "(tried KEYSTORE_PATH, release-key.jks, my-upload-key.jks with alias '$alias'); " +
            "FALLING BACK TO THE DEBUG KEYSTORE. Resulting APK will NOT update in place. " +
            "See docs/RELEASE.md.",
        )
        storeFile = file("${rootDir}/debug.keystore")
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  // Agent G: ProGuard verified: release build uses minifyEnabled + shrinkResources.
  // Baseline Profile generation reference: use androidx.benchmark macrobenchmark module
  // and generate profile with `./gradlew :app:generateBaselineProfile` (or similar script).
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
  implementation(libs.androidx.datastore.preferences)
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
  implementation(libs.hilt.android)
  implementation(libs.dagger)
  ksp(libs.hilt.compiler)
  implementation(libs.kotlinx.serialization.json)
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
  // LeakCanary dependency reference for Agent G performance verification
  debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
  // "ksp"(libs.moshi.kotlin.codegen)
}
