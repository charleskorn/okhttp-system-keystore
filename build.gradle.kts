/*
    Copyright 2017-2022 Charles Korn.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.file.Files

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.6.21"
    id("com.diffplug.spotless") version "6.7.2"
    id("org.ajoberstar.reckon") version "0.16.1"
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    `maven-publish`
    signing
}

repositories {
    mavenCentral()
}

group = "com.charleskorn.okhttp.systemkeystore"

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    api("com.squareup.okhttp3:okhttp:4.9.3")

    testImplementation("io.kotest:kotest-runner-junit5:5.3.0")
    testImplementation("io.kotest:kotest-assertions-core:5.3.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.10.0")
    testImplementation("com.squareup.okhttp3:okhttp-tls:4.9.3")
}

tasks.named<Wrapper>("wrapper") {
    distributionType = Wrapper.DistributionType.ALL
}

kotlin {
    explicitApi()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

java {
    targetCompatibility = JavaVersion.VERSION_1_8
}

val licenseText = Files.readString(project.projectDir.resolve("gradle").resolve("license.txt").toPath())!!
val kotlinLicenseHeader = "/*\n${licenseText.trimEnd().lines().joinToString("\n") { "    $it".trimEnd() }}\n*/\n\n"

spotless {
    val ktlintVersion = "0.45.2"

    kotlin {
        ktlint(ktlintVersion)
        licenseHeader(kotlinLicenseHeader)
    }

    kotlinGradle {
        ktlint(ktlintVersion)
        licenseHeader(kotlinLicenseHeader, "plugins|rootProject|import")
    }
}

tasks.named("spotlessKotlinCheck") {
    mustRunAfter("test")
}

tasks.withType<Test> {
    useJUnitPlatform()

    testLogging {
        showExceptions = true
        showStandardStreams = true
        exceptionFormat = TestExceptionFormat.FULL

        events = setOf(TestLogEvent.FAILED, TestLogEvent.SKIPPED, TestLogEvent.STANDARD_ERROR, TestLogEvent.STANDARD_OUT)
    }
}

reckon {
    scopeFromProp()
    snapshotFromProp()
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://oss.sonatype.org/content/repositories/snapshots/"))
        }
    }

    transitionCheckOptions {
        maxRetries.set(100)
    }
}

tasks.register("publishSnapshot") {
    group = "Publishing"

    dependsOn("publishAllPublicationsToSonatypeRepository")
}

tasks.register("publishRelease") {
    group = "Publishing"

    dependsOn("publishAllPublicationsToSonatypeRepository")
    dependsOn("closeAndReleaseSonatypeStagingRepository")
}

val sourcesJar = tasks.register<Jar>("sourcesJar") {
    from(sourceSets.getByName("main").allSource)
    archiveClassifier.set("sources")
}

val javadocJar = tasks.register<Jar>("javadocJar") {
    from(tasks.named("javadoc"))
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        register<MavenPublication>("JVM") {
            from(components.getByName("kotlin"))
            artifact(sourcesJar)
            artifact(javadocJar)

            pom {
                name.set("okhttp-system-keystore")
                description.set("Automatically use trusted certificates from the operating system keystore (Keychain on macOS, Certificate Store on Windows) with OkHttp")
                url.set("https://github.com/charleskorn/okhttp-system-keystore")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("charleskorn")
                        name.set("Charles Korn")
                        email.set("me@charleskorn.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/charleskorn/okhttp-system-keystore.git")
                    developerConnection.set("scm:git:ssh://github.com:charleskorn/okhttp-system-keystore.git")
                    url.set("https://github.com/charleskorn/okhttp-system-keystore")
                }
            }
        }
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project

    useInMemoryPgpKeys(signingKey, signingPassword)

    sign(publishing.publications)
}
