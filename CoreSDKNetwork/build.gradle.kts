plugins {
    alias(libs.plugins.library)
    alias(libs.plugins.serialization)
    id("maven-publish")
}

android {
    namespace = "xyz.teamgravity.coresdknetwork"

    compileSdk {
        version = release(libs.versions.sdk.compile.get().toInt())
    }

    defaultConfig {
        minSdk = libs.versions.sdk.min.get().toInt()
        consumerProguardFiles.add(file("proguard-rules.pro"))
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
    // serialization
    implementation(libs.serialization)

    // timber
    implementation(libs.timber)

    // gravity core
    implementation(libs.gravity.core)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.raheemadamboev"
            artifactId = "core-sdk-network"
            version = "1.0.0"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}