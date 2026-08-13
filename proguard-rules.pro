# Shared R8 / ProGuard rules for both the app and tv modules.

# --- kotlinx.serialization ---
# The serialization compiler plugin generates X$$serializer classes and a
# Companion.serializer(...) factory that are located reflectively at runtime.
# Without these rules R8 strips or renames them and decoding fails with
# "Serializer for class X is not found" / "Serializer ... has not been
# generated". This covers every @Serializable model in com.notify.core.*.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class com.notify.**$$serializer { *; }
-keepclassmembers class com.notify.** {
    *** Companion;
}
-keepclasseswithmembers class com.notify.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# kotlinx-serialization-json specific lookups.
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Ktor / OkHttp / Coil / Media3 / DataStore / Compose ---
# These ship their own consumer rules with their artifacts, so no extra rules
# are needed here. Ktor resolves serializers through the generated classes kept
# above, and the manifest-referenced Application / Activity / Service classes
# are kept by AGP automatically.
