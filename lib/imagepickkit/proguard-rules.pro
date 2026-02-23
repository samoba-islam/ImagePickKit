# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep data classes used in the public API
-keep class com.samoba.imagepickkit.SelectedImage { *; }
-keep class com.samoba.imagepickkit.ImagePickerConfig { *; }
-keep class com.samoba.imagepickkit.ImagePickerResult { *; }
-keep class com.samoba.imagepickkit.ImagePickerResult$* { *; }
