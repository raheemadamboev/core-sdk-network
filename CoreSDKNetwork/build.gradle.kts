plugins {
    alias(libs.plugins.library)
    id("maven-publish")
}

android {
    namespace = "xyz.teamgravity.coresdknetwork"

    compileSdk {
        version = release(libs.versions.sdk.compile.get().toInt())
    }

    defaultConfig {
        minSdk = libs.versions.sdk.min.get().toInt()
        consumerProguardFiles.add(file("consumer-rules.keep"))
    }

    lint {
        targetSdk = libs.versions.sdk.target.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    // ktor
    api(libs.ktor)
    implementation(libs.ktor.okhttp)
    implementation(libs.ktor.negotiation)
    api(libs.ktor.json)
    api(libs.ktor.logging)

    // timber
    implementation(libs.timber)

    // gravity core
    api(libs.gravity.core)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.raheemadamboev"
            artifactId = "core-sdk-network"
            version = "1.0.3"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}