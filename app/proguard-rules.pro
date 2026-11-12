# VOLTO R8/ProGuard Rules for Release Builds
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*

# Room Database Keep Rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class com.example.persistence.room.** { *; }

# Data Models & Moshi
-keepclassmembers class com.example.types.** { *; }
-keep class com.example.types.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**
