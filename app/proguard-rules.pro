# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Оптимизации для Kotlin
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Оптимизации для Compose
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Оптимизации для Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }
-keepclassmembers class * extends dagger.hilt.android.internal.managers.ViewComponentManager {
    <init>(...);
}

# Оптимизации для Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Оптимизации для Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Оптимизации для OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.internal.platform.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Оптимизации для Coil
-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**

# Оптимизации для DataStore
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# Оптимизации для Location Services
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.location.**

# Оптимизации для Lifecycle
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# Оптимизации для Navigation
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# Оптимизации для Activity Compose
-keep class androidx.activity.compose.** { *; }
-dontwarn androidx.activity.compose.**

# Оптимизации для ViewModel Compose
-keep class androidx.lifecycle.viewmodel.compose.** { *; }
-dontwarn androidx.lifecycle.viewmodel.compose.**

# Оптимизации для Runtime Compose
-keep class androidx.lifecycle.runtime.compose.** { *; }
-dontwarn androidx.lifecycle.runtime.compose.**

# Оптимизации для Hilt Navigation Compose
-keep class androidx.hilt.navigation.compose.** { *; }
-dontwarn androidx.hilt.navigation.compose.**

# Оптимизации для Profile Installer
-keep class androidx.profileinstaller.** { *; }
-dontwarn androidx.profileinstaller.**

# Оптимизации для MultiDex
-keep class androidx.multidex.** { *; }
-dontwarn androidx.multidex.**

# Оптимизации для нашего приложения
-keep class com.example.cf.** { *; }
-keepclassmembers class com.example.cf.** { *; }

# Сохраняем модели данных
-keep class com.example.cf.domain.model.** { *; }
-keepclassmembers class com.example.cf.domain.model.** { *; }

# Сохраняем API сервисы
-keep class com.example.cf.data.remote.** { *; }
-keepclassmembers class com.example.cf.data.remote.** { *; }

# Сохраняем репозитории
-keep class com.example.cf.data.repository.** { *; }
-keepclassmembers class com.example.cf.data.repository.** { *; }

# Сохраняем UI компоненты
-keep class com.example.cf.ui.** { *; }
-keepclassmembers class com.example.cf.ui.** { *; }

# Сохраняем core компоненты
-keep class com.example.cf.core.** { *; }
-keepclassmembers class com.example.cf.core.** { *; }

# Сохраняем DI модули
-keep class com.example.cf.di.** { *; }
-keepclassmembers class com.example.cf.di.** { *; }

# Оптимизации для строковых ресурсов
-keepclassmembers class * {
    @androidx.annotation.StringRes int <fields>;
}

# Оптимизации для цветовых ресурсов
-keepclassmembers class * {
    @androidx.annotation.ColorRes int <fields>;
}

# Оптимизации для drawable ресурсов
-keepclassmembers class * {
    @androidx.annotation.DrawableRes int <fields>;
}

# Удаляем неиспользуемые классы
-dontwarn android.support.**
-dontwarn androidx.legacy.**
-dontwarn org.jetbrains.annotations.**

# Оптимизации для производительности
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# Удаляем логи в релизе
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Оптимизации для строк
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-optimizationpasses 5

# Оптимизации для методов
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Оптимизации для сериализации
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Оптимизации для Parcelable
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# Оптимизации для R8
-allowaccessmodification
-mergeinterfacesaggressively