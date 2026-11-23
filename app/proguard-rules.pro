# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# FocusGuard: TimeInterval is serialized/deserialized reflectively with Gson.
-keep class com.example.scrollstop.TimeInterval { *; }
-keepattributes Signature
