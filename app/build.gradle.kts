plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.deployagram.demo.appa"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.deployagram.demo.appa"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    testOptions {
        unitTests {
            // Robolectric needs the Android resources and manifest on the unit-test classpath.
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    // Ktor is the HTTP client. Its OkHttp engine sends the requests in the app; the tests use a MockEngine instead.
    implementation("io.ktor:ktor-client-core:3.1.3")
    implementation("io.ktor:ktor-client-okhttp:3.1.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.1.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-ktx:1.10.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.18.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.espresso:espresso-core:3.6.1")
    testImplementation("io.ktor:ktor-client-mock:3.1.3")
}

// Robolectric runs Android SDK 36 on the JVM, and that needs Java 21. The app itself still compiles for Java 17.
tasks.withType<Test>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
}
