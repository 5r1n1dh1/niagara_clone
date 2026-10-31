plugins {
  id("library")
}

android {
  namespace = "com.example.core.ui"
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
  implementation(project(":core:common"))
  implementation(project(":core:domain"))
  // Compose BOM required for versionless `libs.androidx.compose.*` aliases.
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.material3)
}
