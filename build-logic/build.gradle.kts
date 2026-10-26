plugins {
  `kotlin-dsl`
}

repositories {
  google()
  mavenCentral()
  gradlePluginPortal()
}



dependencies {
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.10")
  implementation("com.android.application:com.android.application.gradle.plugin:9.1.1")
  implementation("com.android.library:com.android.library.gradle.plugin:9.1.1")
}
