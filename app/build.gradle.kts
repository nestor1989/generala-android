import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// IDs de prueba oficiales de AdMob (https://developers.google.com/admob/android/test-ads)
val testAppId = "ca-app-pub-3940256099942544~3347511713"
val testInterstitialId = "ca-app-pub-3940256099942544/1033173712"
val testRewardedId = "ca-app-pub-3940256099942544/5224354917"

fun prop(name: String): String? = (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() }

val releaseAppId = prop("ADMOB_APP_ID")
val releaseInterstitialId = prop("ADMOB_INTERSTITIAL_ID")
val releaseRewardedId = prop("ADMOB_REWARDED_ID")

val buildingRelease = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }
if (buildingRelease && (releaseAppId == null || releaseInterstitialId == null || releaseRewardedId == null)) {
    throw GradleException(
        "Faltan los IDs reales de AdMob. Completá ADMOB_APP_ID, ADMOB_INTERSTITIAL_ID y " +
            "ADMOB_REWARDED_ID en gradle.properties antes de generar el release."
    )
}

android {
    namespace = "com.nestordelrio.generala"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nestordelrio.generala"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "PRIVACY_URL", "\"https://claude.ai/artifact/GsV9BoFUhKBacPvEnVQYV3\"")
        buildConfigField("String", "REMOVE_ADS_PRODUCT_ID", "\"remove_ads\"")
        buildConfigField("String", "ADMOB_TEST_DEVICE_IDS", "\"${prop("ADMOB_TEST_DEVICE_IDS") ?: ""}\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["admobAppId"] = testAppId
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$testInterstitialId\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"$testRewardedId\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            manifestPlaceholders["admobAppId"] = releaseAppId ?: testAppId
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${releaseInterstitialId ?: testInterstitialId}\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"${releaseRewardedId ?: testRewardedId}\"")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    androidResources {
        // Los archivos del juego se sirven tal cual desde assets/.
        noCompress += listOf("html", "woff2")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    // Play Services trae una versión vieja de Fragment; la forzamos a una compatible con ActivityResult.
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.webkit:webkit:1.14.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    implementation("com.google.android.gms:play-services-ads:25.4.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    implementation("com.android.billingclient:billing:8.3.0")
    implementation("com.google.android.play:review:2.0.2")
}
