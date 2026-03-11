import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
    id("signing")
}

android {
    namespace = "vinz.android.imagesqueeze.lib"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 21

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
    
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Core library dependencies
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.exifinterface)
    implementation(libs.timber)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            afterEvaluate {
                from(components["release"])
            }
            groupId = "io.github.kevinmf1"
            artifactId = "imagesqueeze"
            version = "1.0.0"

            pom {
                name.set("ImageSqueeze")
                description.set("A robust, crash-safe Android image compression library built with Kotlin Coroutines.")
                url.set("https://github.com/kevinmf1/ImageSqueeze")
                
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                
                developers {
                    developer {
                        id.set("kevinmf1")
                        name.set("Kevin Malik Fajar")
                        email.set("kevinmalikf@gmail.com")
                    }
                }
                
                scm {
                    connection.set("scm:git:github.com/kevinmf1/ImageSqueeze.git")
                    developerConnection.set("scm:git:ssh://github.com/kevinmf1/ImageSqueeze.git")
                    url.set("https://github.com/kevinmf1/ImageSqueeze/tree/master")
                }
            }
        }
    }
    
    repositories {
        maven {
            name = "LocalStaging"
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

val localProps = Properties()
val localPropsFile = project.rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localPropsFile.inputStream().use { stream ->
        localProps.load(stream)
    }
}

signing {
    val keyId = localProps.getProperty("signing.keyId")?.takeLast(8)
    val password = localProps.getProperty("signing.password")
    val secretKeyRingFile = localProps.getProperty("signing.secretKeyRingFile")

    if (keyId != null && password != null && secretKeyRingFile != null) {
        project.ext.set("signing.keyId", keyId)
        project.ext.set("signing.password", password)
        project.ext.set("signing.secretKeyRingFile", secretKeyRingFile)
    }
    
    sign(publishing.publications)
}