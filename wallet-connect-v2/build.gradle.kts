import org.gradle.api.JavaVersion
import org.gradle.internal.classpath.Instrumented.systemProperty

plugins {
    id("com.android.library")
    id(libs.plugins.kotlin.android.get().pluginId)
    alias(libs.plugins.sqlDelight)
    alias(libs.plugins.google.ksp)
    id("kotlin-parcelize")
    id("maven-publish")
    id("signing")
}

android {
    namespace = "app.perawallet.walletconnectv2"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        buildConfigField("String", "SDK_VERSION", "\"1.0.0\"")
        buildConfigField("String", "PROJECT_ID", "\"${System.getenv("WC_CLOUD_PROJECT_ID") ?: ""}\"")
        buildConfigField("Integer", "TEST_TIMEOUT_SECONDS", "${System.getenv("TEST_TIMEOUT_SECONDS") ?: 30}")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments += mutableMapOf("clearPackageData" to "true")

        File("${rootDir.path}/gradle/consumer-rules").listFiles()?.let { proguardFiles ->
            consumerProguardFiles(*proguardFiles)
        }

        ndk.abiFilters += listOf("armeabi-v7a", "x86", "x86_64", "arm64-v8a")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "${rootDir.path}/gradle/proguard-rules/sdk-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
        freeCompilerArgs = listOf("-Xcontext-receivers")
    }

    sourceSets {
        getByName("test").resources.srcDirs("src/test/resources")
    }

    buildFeatures {
        buildConfig = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

sqldelight {
    databases {
        create("AndroidCoreDatabase") {
            packageName.set("app.perawallet.walletconnectv2.sdk.storage.data.dao")
            srcDirs.setFrom("src/main/sqldelight/core")
            schemaOutputDirectory.set(file("src/main/sqldelight/core/databases"))
            verifyMigrations.set(true)
        }

        create("SignDatabase") {
            packageName.set("app.perawallet.walletconnectv2.sign.storage.data.dao")
            srcDirs.setFrom("src/main/sqldelight/sign")
            schemaOutputDirectory.set(file("src/main/sqldelight/sign/databases"))
            verifyMigrations.set(true)
            verifyDefinitions.set(true)
        }
    }
}

dependencies {
    ksp(libs.moshi.ksp)

    api(libs.coroutines)
    api(libs.koin.android)
    api(libs.bundles.scarlet)

    implementation(libs.scarlet.android)
    implementation(libs.bundles.sqlDelight)
    implementation(libs.sqlCipher)
    implementation(libs.relinker)
    implementation(libs.androidx.security)
    implementation(libs.timber)
    implementation(libs.web3jCrypto)
    implementation(libs.bundles.kethereum)
    implementation(libs.bundles.retrofit)
    implementation(libs.okhttp.logging)
    implementation(libs.mulitbase)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.moshi.adapters)
}

publishing {
    publications {
        create<MavenPublication>("release") {
            // Safe lazy evaluation for Android components
            afterEvaluate {
                from(components["release"])
            }

            groupId = "app.perawallet"
            artifactId = "pera-wallet-connect-v2"
            version = libs.versions.peraWalletConnect.get()

            pom {
                name.set("Pera Wallet Connect V2")
                description.set("WalletConnect V2 SDK for Android")
                url.set("https://github.com/perawallet/PeraWalletConnect")

                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                developers {
                    developer {
                        id.set("perawallet")
                        name.set("Pera Wallet")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/perawallet/PeraWalletConnect.git")
                    developerConnection.set("scm:git:ssh://github.com/perawallet/PeraWalletConnect.git")
                    url.set("https://github.com/perawallet/PeraWalletConnect")
                }
            }
        }
    }

    repositories {
        mavenLocal()
    }
}