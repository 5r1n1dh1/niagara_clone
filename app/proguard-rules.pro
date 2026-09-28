# Add project specific ProGuard rules here.
# Preserve Line Numbers for stack traces
-keepattributes SourceFile,LineNumberTable,*Annotation*
-renamesourcefileattribute SourceFile

# Data Models & Enums
-keepclassmembers enum * { *; }
-keep class com.example.data.** { *; }
-keepclassmembers class com.example.data.** { *; }

# Android AppWidget & RemoteViews
-keep class * extends android.appwidget.AppWidgetProvider { *; }
-keep class * extends android.appwidget.AppWidgetHost { *; }
-keep class * extends android.appwidget.AppWidgetHostView { *; }

# Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**
