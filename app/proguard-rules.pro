# ARCore
-keep class com.google.ar.core.** { *; }
-keep class com.google.ar.** { *; }

# App classes
-keep class com.ardoom.** { *; }

# Kotlin metadata
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Material Components
-keep class com.google.android.material.** { *; }

# AndroidX
-keep class androidx.** { *; }
