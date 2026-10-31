plugins {
  id("library")
}

android {
  namespace = "com.example.core.data"
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
  // Convention plugin applies the Compose compiler; runtime must be on the classpath.
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(project(":core:common"))
  implementation(project(":core:domain"))
  implementation(libs.androidx.datastore.preferences)
}
