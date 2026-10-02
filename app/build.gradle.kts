plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services) // Para que Firebase funcione
}

android {
    namespace = "com.miguel.unitask"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.miguel.unitask"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Activamos ViewBinding para no usar el viejo findViewById y evitar errores de "null"
    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Las de cajón para que la app se vea bien (Material Design)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    
    // El cerebro de la navegación entre pantallas
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    // El combo de Firebase para la nube (Usuarios, Base de datos y Fotos)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.database)
    
    // Retrofit: El cartero que va a buscar las noticias a internet
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Glide: Lo usamos porque bajar fotos de internet es difícil y esta librería lo hace en una línea
    implementation(libs.glide)

    // Para el gesto de "jalar para actualizar" la lista
    implementation(libs.androidx.swiperefreshlayout)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
