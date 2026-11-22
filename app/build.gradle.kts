plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "br.com.splitscreenpro"
    compileSdk = 35
    defaultConfig {
        applicationId = "br.com.splitscreenpro"
        minSdk = 32
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
