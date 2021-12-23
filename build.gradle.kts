/*
    Copyright 2017-2021 Charles Korn.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

import java.nio.file.Files

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
    id("com.diffplug.spotless") version "6.0.5"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

tasks.named<Wrapper>("wrapper") {
    distributionType = Wrapper.DistributionType.ALL
}

java {
    targetCompatibility = JavaVersion.VERSION_1_8
}

val licenseText = Files.readString(project.projectDir.resolve("gradle").resolve("license.txt").toPath())!!
val kotlinLicenseHeader = "/*\n${licenseText.trimEnd().lines().joinToString("\n") { "    $it".trimEnd() }}\n*/\n\n"

spotless {
    val ktlintVersion = "0.43.2"

    kotlin {
        ktlint(ktlintVersion)
        licenseHeader(kotlinLicenseHeader)
    }

    kotlinGradle {
        ktlint(ktlintVersion)
        licenseHeader(kotlinLicenseHeader, "plugins|rootProject|import")
    }
}
