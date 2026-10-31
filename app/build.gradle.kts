plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android { namespace = "com.perchikhot.novagame"; compileSdk = 35
 defaultConfig { applicationId = "com.perchikhot.novagame"; minSdk = 26; targetSdk = 35; versionCode = 13; versionName = "0.10.0" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
