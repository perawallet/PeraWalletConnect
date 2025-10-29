import org.gradle.api.JavaVersion

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    id("maven-publish")
    id("signing")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    // --- Hex encoding ---
    implementation(libs.khex.core)
    implementation(libs.khex.extensions)

    // --- Cryptography ---
    implementation(libs.bcprov)

    // --- JSON ---
    implementation(libs.gson)
    implementation(libs.moshi)

    // --- HTTP ---
    implementation(libs.okhttp)
}

// ✅ PUBLISHING CONFIGURATION
publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])

            groupId = "app.perawallet"
            artifactId = "pera-wallet-connect-v1"
            version = libs.versions.peraWalletConnect.get()

            pom {
                name.set("Pera Wallet Connect V1")
                description.set("WalletConnect V1 SDK for Android")
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
}

afterEvaluate {
    extensions.findByType<SigningExtension>()?.apply {
        val key = System.getenv("GPG_PRIVATE_KEY")
        val password = System.getenv("GPG_PRIVATE_KEY_PASSWORD")

        if (!key.isNullOrBlank() && !password.isNullOrBlank()) {
            useInMemoryPgpKeys(key, password)
            sign(publishing.publications)
        }
    }
}