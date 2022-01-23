# okhttp-system-keystore

[![CI](https://github.com/charleskorn/okhttp-system-keystore/actions/workflows/ci.yml/badge.svg)](https://github.com/charleskorn/okhttp-system-keystore/actions/workflows/ci.yml)
[![License](https://img.shields.io/github/license/charleskorn/okhttp-system-keystore.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/com.charleskorn.okhttp.systemkeystore/okhttp-system-keystore.svg?label=maven%20central)](https://search.maven.org/search?q=g:%22com.charleskorn.okhttp.systemkeystore%22%20AND%20a:%22okhttp-system-keystore%22)

Automatically use trusted certificates from the operating system native certificate trust system (Keychain on macOS, Certificate Store on Windows) with [OkHttp](https://github.com/square/okhttp).

## Why would you want to do this?

Many organisations configure encryption-terminating proxies to intercept, examine and block traffic flowing
through their networks. In order to do this, they must add a trusted root CA certificate to all client machines, or else any HTTPs traffic
will be flagged as using an untrusted certificate. In most situations, this certificate is only added to the operating system's native
certificate trust system, and not Java's.

This library provides a convenience method to configure OkHttp to use the operating system's native certificate trust system in addition to
the JVM's built-in trusted certificates.

## Setup

In Gradle:

```kotlin
dependencies {
  implementation("com.charleskorn.okhttp.systemkeystore:okhttp-system-keystore:<version number here>") // Get the latest version number from https://github.com/charleskorn/okhttp-system-keystore/releases/latest
}
```

Check the [releases page](https://github.com/charleskorn/okhttp-system-keystore/releases) for the latest release information, and the 
[Maven Central page](https://search.maven.org/artifact/com.charleskorn.okhttp.systemkeystore/okhttp-system-keystore) for examples of how to reference the library in other build systems.

## Usage

Call `useOperatingSystemCertificateTrustStore()` when building your OkHttp client:

```kotlin
import com.charleskorn.okhttp.systemkeystore

val client = OkHttpClient.Builder()
    .useOperatingSystemCertificateTrustStore()
    .build()
```

## Behaviour

On Windows: uses both user-trusted and machine-trusted root CA certificates, in addition to the local JVM's built-in trusted certificates.

On macOS: uses trusted certificates from the user's `login` keychain, in addition to the local JVM's built-in trusted certificates.

On all other operating systems: uses only the local JVM's built-in trusted certificates (which is what OkHttp uses by default).

## Contributing

This project uses Gradle. 

Run linting and tests with `./gradlew check`.

### macOS-specific notes

The tests need to temporarily add certificates to your local keychain. Therefore, when the tests run you will need to approve adding each certificate (two in total) by entering your password or using Touch ID. 

### Windows-specific notes

The tests need to temporarily add a certificate trusted at the machine-wide level. Therefore, you must run tests from an elevated (administrator) terminal.

If you are using an elevated terminal and still encounter issues, try disabling the Gradle daemon with `--no-daemon`, for example. `./gradlew --no-daemon check`.
(The Gradle daemon might have started un-elevated, disabling the daemon ensures that it runs with the same level of access as your terminal.)
