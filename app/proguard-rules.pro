# Add project specific ProGuard rules here.
-keepclassmembers class com.omo.manager.native.** {
    native <methods>;
}
-keep class com.omo.manager.native.** { *; }
