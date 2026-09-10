# ProGuard / R8 Rules for OmniChat

# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class * extends okhttp3.Response { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }
-keep class com.example.domain.model.** { *; }
-keep class com.example.data.dto.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class androidx.room.** { *; }
-keep class com.example.data.local.entity.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-dontwarn com.bumptech.glide.**

# Media3 ExoPlayer
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.ui.** { *; }
-dontwarn androidx.media3.**

# AndroidX Security Crypto
-keep class androidx.security.crypto.** { *; }
