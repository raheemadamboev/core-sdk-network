plugins {
    alias(libs.plugins.library) apply false
}

buildscript {
    dependencies {
        classpath(libs.kotlin)
    }
}