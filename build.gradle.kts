import org.gradle.api.publish.PublishingExtension
import org.gradle.plugins.signing.SigningExtension

plugins {
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.versions.plugin)
    alias(libs.plugins.mavenPublish)
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

nmcpAggregation {
    centralPortal {
        username = System.getenv("CENTRAL_PORTAL_USERNAME")
        password = System.getenv("CENTRAL_PORTAL_TOKEN")
        publishingType = "AUTOMATIC"
    }

    publishAllProjectsProbablyBreakingProjectIsolation()
}

subprojects {
    plugins.withId("maven-publish") {
        project.pluginManager.apply("signing")

        project.extensions.configure<SigningExtension> {
            val publishing = project.extensions.findByType(PublishingExtension::class.java)
            val key = System.getenv("GPG_PRIVATE_KEY")
            val password = System.getenv("GPG_PRIVATE_KEY_PASSWORD")

            if (!key.isNullOrBlank() && !password.isNullOrBlank()) {
                useInMemoryPgpKeys(key, password)
                sign(publishing?.publications)
            }
        }
    }
}