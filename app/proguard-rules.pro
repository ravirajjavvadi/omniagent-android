# OmniAgent ProGuard Rules

# Keep Chaquopy Python modules
-keep class com.chaquo.python.** { *; }
-dontwarn com.chaquo.python.**

# Keep Room database entities
-keep class com.omniagent.app.data.model.** { *; }
-keep class com.omniagent.app.data.db.** { *; }

# Keep Gson serialization
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }
