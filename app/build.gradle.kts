plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.gwallet"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.gwallet"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.github.blikoon:QRCodeScanner:0.1.2")

    // ML Kit Barcode Scanning (latest version)
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
// Play Services Code Scanner
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.android.volley:volley:1.2.1")


}