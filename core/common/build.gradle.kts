plugins {
  id("library")
}

android {
  namespace = "com.example.core.common"
  compileSdk = 36
  defaultConfig {
    minSdk = 35
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
  }
}

dependencies {
  // Compose BOM required for versionless `libs.androidx.compose.*` aliases.
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.compose.ui)
}
