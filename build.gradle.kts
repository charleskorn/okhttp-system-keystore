plugins {
    id("org.jetbrains.kotlin.jvm") version "1.5.31"
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
