plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.vecinosotello"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.vecinosotello"
        minSdk = 27
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Solo para el manifiesto de las pruebas con Robolectric; la app usa la clave real del plugin de secretos
        manifestPlaceholders["MAPS_API_KEY"] = "DEFAULT_API_KEY"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true // Robolectric
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        // El plugin de secretos (clave de Maps) escribe en BuildConfig; sin esto avisa "has no effect"
        buildConfig = true
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.play.services.maps)
    // Firebase (proyecto vecinosotello): cuentas, datos en tiempo real y notificaciones de anuncios
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-messaging")
    // MQTT para las alertas: cliente Eclipse Paho (el mismo que usa por dentro la librería de la guía),
    // ejecutado en nuestro servicio en primer plano. La versión Android 3.3.5 de la guía registra un
    // receptor sin el flag que exige Android 14+ con targetSdk 34 o más, y cerraría la app.
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")

    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("org.mockito:mockito-core:5.18.0")
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}

secrets {
    propertiesFileName = "local.properties"
    defaultPropertiesFileName = "local.defaults.properties"
}
