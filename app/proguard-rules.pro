# Android ProGuard rules for Folder Tree Snapshot

# Basic ProGuard rules for Android
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Keep all Activities, Services, BroadcastReceivers
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver

# Keep all ViewModels
-keep class * extends androidx.lifecycle.ViewModel

# Keep all data model classes for serialization
-keep class com.example.fts.data.model.** { *; }

# Keep all repository classes
-keep class com.example.fts.data.repository.** { *; }

# Keep all domain classes
-keep class com.example.fts.domain.** { *; }

# Keep all UI classes
-keep class com.example.fts.ui.** { *; }

# Keep all utility classes
-keep class com.example.fts.util.** { *; }

# Keep all service classes
-keep class com.example.fts.service.** { *; }

# Keep all SAF related classes
-keep class com.example.fts.data.saf.** { *; }

# Keep all cache related classes
-keep class com.example.fts.data.cache.** { *; }

# Kotlin specific rules
-keep class kotlin.Metadata { *; }
-keep class kotlin.** { *; }
-keep class androidx.** { *; }

# Keep R classes
-keep class **.R$* { *; }