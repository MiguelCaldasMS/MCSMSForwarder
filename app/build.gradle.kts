// app/build.gradle.kts

// Apply plugins using aliases defined in libs.versions.toml
// The versions are sourced from the [plugins] section of the TOML file.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Add other plugins as needed, e.g.:
    // alias(libs.plugins.google.devtools.ksp)
    // alias(libs.plugins.google.gms.googleServices)
}

android {
    // Namespace is mandatory
    namespace = "com.miguelcaldas.mcsmsforwarder1"
    // Compile SDK version from the version catalog
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        // Application ID can often be omitted if it's the same as the namespace
        applicationId = "com.miguelcaldas.mcsmsforwarder1"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Use 'is' prefix for boolean setters
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        viewBinding = true
    }
    // 'buildToolsVersion' is generally not needed anymore.
    // AGP selects a suitable version based on compileSdk.
}

dependencies {
    // Dependencies using aliases from libs.versions.toml
    implementation(libs.kotlin.stdlib) // Explicit stdlib, version from catalog
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.work.runtime.ktx)

    // Test dependencies (uncomment and add if you use them, ensure they are in libs.versions.toml)
    // testImplementation(libs.junit)
    // androidTestImplementation(libs.androidx.test.ext.junit)
    // androidTestImplementation(libs.androidx.test.espresso.core)
}
