# Android ProGuard rules
-keep class androidx.** { *; }
-keep class com.google.android.material.** { *; }
-keep class android.support.** { *; }

# Kotlin
-keep class kotlin.Metadata { *; }
-keep class kotlin.** { *; }

# Coroutines
-keep class kotlinx.coroutines.** { *; }

# Serialization
-keep class kotlinx.serialization.** { *; }

# Keep all activities, services, and receivers
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep data models for serialization
-keep class com.example.fts.data.model.** { *; }

# Keep R class
-keep class **.R$* { *; }