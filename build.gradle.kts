import io.github.gradlenexus.publishplugin.NexusPublishExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.plugins.signing.SigningExtension

plugins {
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.versions.plugin)
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

configure<NexusPublishExtension> {
    repositories {
        sonatype {
            username.set(System.getenv("OSSRH_USERNAME"))
            password.set(System.getenv("OSSRH_PASSWORD"))
        }
    }
}

subprojects {
    plugins.withId("maven-publish") {

        // 🔹 Configure Maven repositories
        extensions.configure<PublishingExtension> {
            repositories {
                mavenLocal()

                maven {
                    name = "OSSRH"
                    url = uri(
                        if (version.toString().endsWith("SNAPSHOT"))
                            "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                        else
                            "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                    )
                    credentials {
                        username = System.getenv("OSSRH_USERNAME")
                        password = System.getenv("OSSRH_PASSWORD")
                    }
                }
            }
        }

        project.plugins.withId("signing") {
            project.extensions.configure<SigningExtension> {
                val publishing = project.extensions.findByType(PublishingExtension::class.java)
                if (publishing != null) {
                    useInMemoryPgpKeys(
                        System.getenv("GPG_PRIVATE_KEY"),
                        System.getenv("GPG_PRIVATE_KEY_PASSWORD")
                    )
                    sign(publishing.publications)
                }
            }
        }
    }
}