plugins {
  id("library")
}

android {
  namespace = "com.example.system.accessibility"
  compileSdk = 36
  defaultConfig {
    minSdk = 24
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = false
  }
}

dependencies {
  implementation(project(":core:domain"))
}
