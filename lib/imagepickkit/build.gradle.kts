import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    `maven-publish`
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.samoba-islam"
                artifactId = "imagepickkit"
                version = "1.0.0"
            }
        }
    }
}

android {
    namespace = "com.samoba.imagepickkit"
    compileSdk = 36

    defaultConfig {
        minSdk = 29

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(imagepickkitLibs.androidx.core.ktx)
    implementation(imagepickkitLibs.androidx.activity.compose)
    implementation(platform(imagepickkitLibs.androidx.compose.bom))
    implementation(imagepickkitLibs.androidx.compose.ui)
    implementation(imagepickkitLibs.androidx.compose.ui.graphics)
    implementation(imagepickkitLibs.androidx.compose.ui.tooling.preview)
    implementation(imagepickkitLibs.androidx.compose.material3)
    
    // Material Icons Extended
    implementation(imagepickkitLibs.androidx.compose.material.icons.extended)
    
    // Paging
    implementation(imagepickkitLibs.androidx.paging.common)
    implementation(imagepickkitLibs.androidx.paging.compose)
    
    // Kotlin immutable collections
    implementation(imagepickkitLibs.kotlinx.collections)
    
    // Lifecycle
    implementation(imagepickkitLibs.androidx.lifecycle.runtime.ktx)
    implementation(imagepickkitLibs.androidx.lifecycle.viewmodel.compose)
    
    // AVIF/HEIF software decoder (for devices without hardware support)
    implementation(imagepickkitLibs.avif.coder)
    
    testImplementation(imagepickkitLibs.junit)
    androidTestImplementation(imagepickkitLibs.androidx.junit)
    androidTestImplementation(imagepickkitLibs.androidx.espresso.core)
}
