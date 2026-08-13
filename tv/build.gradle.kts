plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

fun envOrNull(name: String): String? =
    (System.getenv(name) ?: providers.gradleProperty(name).orNull)?.takeIf { it.isNotBlank() }

fun keystoreFile(): File? {
    val path = envOrNull("KEYSTORE_PATH") ?: return null
    return File(path).takeIf { it.isFile() }
}

// Release CI passes the tag through as -PappVersionName / -PappVersionCode so
// the APK reports the same version Obtainium reads off the GitHub release.
// Local and PR builds fall back to the placeholder below.
val appVersionName = (findProperty("appVersionName") as String?) ?: "0.0.0"
val appVersionCode = (findProperty("appVersionCode") as String?)?.toInt() ?: 1

android {
    namespace = "com.notify.android.tv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.notify.android.tv"
        minSdk = 24
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
    }

    // Signing comes from environment variables (CI: GitHub secrets) so no
    // keystore ever needs to be committed. If any credential is missing the
    // release build is produced unsigned instead of failing the build.
    signingConfigs {
        create("release") {
            storeFile = keystoreFile()
            storePassword = envOrNull("KEYSTORE_PASSWORD")
            keyAlias = envOrNull("KEY_ALIAS")
            keyPassword = envOrNull("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                rootProject.file("proguard-rules.pro")
            )
            val cfg = signingConfigs.getByName("release")
            if (cfg.storeFile != null &&
                cfg.storePassword != null &&
                cfg.keyAlias != null &&
                cfg.keyPassword != null
            ) {
                signingConfig = cfg
            }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.coil.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
