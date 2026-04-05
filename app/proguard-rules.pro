# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in ${sdk.dir}/tools/proguard/proguard-android.txt

# Gson: keep data classes used for serialization
-keepclassmembers class com.dailychallenge.app.data.** { *; }
-keep class com.dailychallenge.app.data.DayRecord { *; }
-keep class com.dailychallenge.app.data.CloudSaveData { *; }
-keep class com.dailychallenge.app.data.CloudPrefsData { *; }
-keep class com.dailychallenge.app.data.CustomGoalJson { *; }
-keep class com.dailychallenge.app.data.PendingChallenge { *; }

# Gson internals
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Google Play Games Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Google Play Billing
-keep class com.android.billingclient.** { *; }

# Google In-App Review
-keep class com.google.android.play.core.** { *; }
