# Reglas de ProGuard para la build de release del empaquetado nativo (jpackage vía Compose
# Desktop). Sin estas reglas, ProGuard elimina/renombra clases a las que solo se accede por
# reflexión (Room genera su implementación en tiempo de compilación pero se instancia con
# Class.forName en tiempo de ejecución; kotlinx.serialization, Ktor y xmlutil también usan
# reflexión/carga dinámica de clases en varios puntos).

# --- Room: mantener la clase de base de datos y su implementación generada por KSP ---
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep,allowobfuscation @androidx.room.Database class * { *; }
-keep class **_Impl { *; }
-keep interface androidx.room.** { *; }
-dontwarn androidx.room.paging.**

# --- Métodos JNI nativos (androidx.sqlite.driver.bundled usa una librería nativa de SQLite vía
# JNI): ProGuard no debe optimizar/renombrar estas declaraciones porque su firma binaria debe
# coincidir exactamente con la librería nativa (.so/.dll) empaquetada.
-keepclasseswithmembers,includedescriptorclasses class * {
    native <methods>;
}
-keep class androidx.sqlite.driver.bundled.** { *; }
-dontwarn androidx.sqlite.**

# --- Entidades/DAOs anotados con Room: conservar nombres y miembros para el mapeo de columnas ---
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# --- kotlinx.serialization: mantener los serializadores generados y companion objects ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class es.bancamarch.colorblind3d.**$$serializer { *; }
-keepclassmembers class es.bancamarch.colorblind3d.** {
    *** Companion;
}
-keepclasseswithmembers class es.bancamarch.colorblind3d.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Ktor: el motor HTTP (CIO) y el soporte de kotlinx.serialization se descubren en tiempo de
# ejecución vía java.util.ServiceLoader (ficheros META-INF/services); si ProGuard elimina o
# renombra esas clases de implementación, el ServiceLoader falla con
# "Provider ... not found" aunque compile sin problemas.
-keep class io.ktor.client.engine.cio.** { *; }
-keep class io.ktor.client.engine.HttpClientEngineContainer
-keep interface io.ktor.client.engine.HttpClientEngineContainer
-keep class * implements io.ktor.client.engine.HttpClientEngineContainer { *; }
-keep class io.ktor.serialization.kotlinx.json.** { *; }
-keep interface io.ktor.serialization.kotlinx.KotlinxSerializationExtensionProvider
-keep class * implements io.ktor.serialization.kotlinx.KotlinxSerializationExtensionProvider { *; }
-keep,includedescriptorclasses class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn org.slf4j.**
-dontnote io.ktor.**

# --- xmlutil: intenta cargar distintos backends de streaming según la plataforma disponible ---
-keep class nl.adaptivity.xmlutil.** { *; }
-dontwarn nl.adaptivity.xmlutil.**

# --- Coil3: usa reflexión/ServiceLoader para descubrir decodificadores y fetchers ---
-keep class coil3.** { *; }
-dontwarn coil3.**

# --- Punto de entrada de la aplicación ---
-keep class es.bancamarch.colorblind3d.MainKt { *; }
