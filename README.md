# okhttp-system-keystore is no longer maintained

I am no longer actively using okhttp-system-keystore and do not have time to maintain it properly, and so this project is now archived.

The source code and published artifacts will remain available for the foreseeable future.

Maintained forks are welcome and encouraged.


# okhttp-system-keystore

[![CI](https://github.com/charleskorn/okhttp-system-keystore/actions/workflows/ci.yml/badge.svg)](https://github.com/charleskorn/okhttp-system-keystore/actions/workflows/ci.yml)
[![License](https://img.shields.io/github/license/charleskorn/okhttp-system-keystore.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/com.charleskorn.okhttp.systemkeystore/okhttp-system-keystore.svg?label=maven%20central)](https://search.maven.org/artifact/com.charleskorn.okhttp.systemkeystore/okhttp-system-keystore)

Automatically use trusted certificates from the operating system native certificate trust system with [OkHttp](https://github.com/square/okhttp).

Supports Keychain on macOS and Certificate Store on Windows, in addition to any certificates in the local JVM's default keystore.

## Why would you want to do this?

There are a couple of scenarios where using the operating system certificate trust system can be useful:

* when communicating with servers that present a self-signed certificate, such as local test servers

* when communicating with servers that present a certificate signed by a private certificate authority (CA), such as private services signed by an 
  organisation's internal CA
  
* when communicating with servers via an encryption-terminating proxy, which is common in corporate environments where network administrators
  want to be able to intercept and examine all encrypted traffic flowing through their network

In all of these scenarios, it's usually easier to add the certificate required to the operating system's certificate trust store rather than the local JVM's
default keystore. However, by default, JVM-based applications use only the JVM's default keystore, which means connecting to servers will fail due to not
trusting the certificate presented. 

This library provides a convenience method to configure OkHttp to use the operating system's native certificate trust system in addition to
the JVM's default keystore, allowing your application to communicate in situations such as these while still verifying that the certificate presented is trustworthy.

## Setup

In Gradle:

```kotlin
dependencies {
  implementation("com.charleskorn.okhttp.systemkeystore:okhttp-system-keystore:<version number here>") // Get the latest version number from https://github.com/charleskorn/okhttp-system-keystore/releases/latest
}
```

Check the [releases page](https://github.com/charleskorn/okhttp-system-keystore/releases) for the latest release information, and the 
[Maven Central page](https://search.maven.org/artifact/com.charleskorn.okhttp.systemkeystore/okhttp-system-keystore) for examples of how to reference the library in other build systems.

### macOS security note

:warning: On macOS, it's highly recommended that this library only be used with versions of the JDK that contain a fix for JDK-8278449
("Only Expose Certificates With Proper Trust Settings as Trusted Certificate Entries in macOS KeychainStore").

Without this fix, certificates marked as 'never trust' in your certificate trust settings will be treated as trusted by Java. 

The following versions of the JDK contain a fix for this issue:

* JDK 8: 8u332 or later
* JDK 11: 11.0.15 or later
* JDK 17: 17.0.3 or later
* JDK 18: 18.0.1 or later
* All versions of JDK 19 or later

## Usage

Call `useOperatingSystemCertificateTrustStore()` when building your OkHttp client:

```kotlin
import com.charleskorn.okhttp.systemkeystore

val client = OkHttpClient.Builder()
    .useOperatingSystemCertificateTrustStore()
    .build()
```

## Behaviour

On Windows: uses both user-trusted and machine-trusted root CA certificates, in addition to the local JVM's default keystore.

On macOS: uses trusted certificates from the user's `login` keychain, in addition to the local JVM's default keystore.

On all other operating systems: uses only the local JVM's default keystore (which is what OkHttp uses by default).

## Contributing

This project uses Gradle. 

Run linting and tests with `./gradlew check`.

### macOS-specific notes

The tests need to temporarily add certificates to your local keychain. Therefore, when the tests run you will need to approve adding each certificate (two in total) by entering your password or using Touch ID. 

### Windows-specific notes

The tests need to temporarily add a certificate trusted at the machine-wide level. Therefore, you must run tests from an elevated (administrator) terminal.

If you are using an elevated terminal and still encounter issues, try disabling the Gradle daemon with `--no-daemon`, for example. `./gradlew --no-daemon check`.
(The Gradle daemon might have started un-elevated, disabling the daemon ensures that it runs with the same level of access as your terminal.)
