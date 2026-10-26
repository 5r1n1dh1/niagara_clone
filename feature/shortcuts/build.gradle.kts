plugins {
  id("library")
}

android {
  namespace = "com.example.feature.shortcuts"
  compileSdk = 36
  defaultConfig {
    minSdk = 24
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
  implementation(project(":core:domain"))
  implementation(project(":core:ui"))
}
