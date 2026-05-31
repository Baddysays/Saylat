import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.isFile) f.inputStream().use { load(it) }
}
// Личный сервер — только в local.properties, не в репозитории (см. local.properties.example)
val saylatServerUrl: String = localProps.getProperty("saylat.server.url", "").trim()
val saylatServerUrlField = "\"${saylatServerUrl.replace("\"", "\\\"")}\""
val saylatApiKey: String = localProps.getProperty("saylat.api.key", "").trim()
val saylatApiKeyField = "\"${saylatApiKey.replace("\"", "\\\"")}\""

android {
    namespace = "com.baddysays.saylat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.baddysays.saylat"
        minSdk = 26
        targetSdk = 35
        versionCode = 49
        versionName = "0.5.40"
        buildConfigField("String", "PUBLIC_SERVER_URL", saylatServerUrlField)
        buildConfigField("String", "PROXY_API_KEY", saylatApiKeyField)
        buildConfigField(
            "String",
            "GITHUB_UPDATE_JSON",
            "\"https://raw.githubusercontent.com/Baddysays/Saylat/main/releases/update.json\"",
        )
    }

    buildTypes {
        debug {
            buildConfigField("String", "DEFAULT_PROXY_URL", "\"http://10.0.2.2:8787\"")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("String", "DEFAULT_PROXY_URL", saylatServerUrlField)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
