plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
 namespace = "dev.glucoselog.phone"
 compileSdk = 36
 defaultConfig { applicationId = "dev.glucoselog.phone"; minSdk = 28; targetSdk = 35; versionCode = 2; versionName = "0.2.0"; testInstrumentationRunner = "dev.glucoselog.phone.DeviceChecks" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies {
 implementation("androidx.activity:activity-ktx:1.10.1")
 implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
 implementation("androidx.health.connect:connect-client:1.1.0")
 testImplementation("junit:junit:4.13.2")
}
