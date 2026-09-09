import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("io.sentry.android.gradle") version "6.21.0"
}

android {
    namespace = "com.ratio.launcher"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bloedboemmel.ratio"
        minSdk = 26
        targetSdk = 36
        versionCode = 13
        versionName = "1.1"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["sentryEnvironment"] = "debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            manifestPlaceholders["sentryEnvironment"] = "production"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("io.sentry:sentry-android:8.43.2")
}


sentry {
    org.set("memyselfandi-9i")
    projectName.set("ratio")

    // Only upload sources when auth token is available (CI)
    autoUploadSourceContext.set(System.getenv("SENTRY_AUTH_TOKEN") != null)

    // this will upload your source code to Sentry to show it as part of the stack traces
    // disable if you don't want to expose your sources
    includeSourceContext.set(true)
}
