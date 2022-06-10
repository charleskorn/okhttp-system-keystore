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

package com.charleskorn.okhttp.systemkeystore

import okhttp3.tls.HeldCertificate
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

internal abstract class TrustedCertificateContainer(val certificate: TestCertificate) : AutoCloseable {
    val heldCertificate: HeldCertificate
        get() = certificate.heldCertificate

    private lateinit var certificatePath: Path

    fun addToLocalTrustStore() {
        certificatePath = writeToTemporaryFile()
        addToLocalTrustStore(certificatePath)
    }

    private fun writeToTemporaryFile(): Path {
        val path = Files.createTempFile("okhttp-systemkeystore-${certificate.commonName}", ".pem")

        Files.write(path, heldCertificate.certificatePem().toByteArray(Charsets.UTF_8))

        return path
    }

    override fun close() {
        removeFromLocalTrustStore(certificatePath)
        removeCertificateFile()
    }

    private fun removeCertificateFile() {
        Files.delete(certificatePath)
    }

    protected abstract fun addToLocalTrustStore(certificatePath: Path)
    protected abstract fun removeFromLocalTrustStore(certificatePath: Path)
}

// This adds a certificate at the user level.
internal class MacKeychainTrustedCertificateContainer(certificate: TestCertificate) : TrustedCertificateContainer(certificate) {
    override fun addToLocalTrustStore(certificatePath: Path) {
        runProcess("security", "add-trusted-cert", "-p", "ssl", "-r", "trustRoot", "-k", userKeychainPath, certificatePath.toString())
    }

    override fun removeFromLocalTrustStore(certificatePath: Path) {
        runProcess("security", "remove-trusted-cert", certificatePath.toString())
        runProcess("security", "delete-certificate", "-c", certificate.commonName, userKeychainPath)
    }

    companion object {
        private val userKeychainPath = System.getProperty("user.home")!! + "/Library/Keychains/login.keychain-db"

        fun createAndTrust(commonName: String, isCertificateAuthority: Boolean = false): TrustedCertificateContainer {
            val certificate = TestCertificate(commonName, isCertificateAuthority)
            return MacKeychainTrustedCertificateContainer(certificate).also { it.addToLocalTrustStore() }
        }
    }
}

// This adds a certificate at the machine level.
// FIXME: Adding certificates at the user level requires clicking 'Yes' on a dialog, and there's no way to avoid this, making automated tests painful.
// I've manually verified that certificates trusted at the user level are also trusted by the Windows-ROOT JVM KeyStore (see the tests that were
// removed in the commit where this comment was added).
internal class WindowsTrustedCertificateContainer(certificate: TestCertificate) : TrustedCertificateContainer(certificate) {
    override fun addToLocalTrustStore(certificatePath: Path) {
        runProcess("certutil", "-addstore", "root", certificatePath.toString())
    }

    override fun removeFromLocalTrustStore(certificatePath: Path) {
        runProcess("certutil", "-delstore", "root", certificatePath.toString())
    }

    companion object {
        fun createAndTrust(commonName: String, isCertificateAuthority: Boolean = false): TrustedCertificateContainer {
            val certificate = TestCertificate(commonName, isCertificateAuthority)
            return WindowsTrustedCertificateContainer(certificate).also { it.addToLocalTrustStore() }
        }
    }
}

private fun runProcess(vararg args: String) {
    val process = ProcessBuilder()
        .command(*args)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .redirectErrorStream(true)
        .start()

    try {
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            throw RuntimeException("Process '${args.joinToString(" ")}' timed out with output: ${process.outputText}")
        }

        if (process.exitValue() != 0) {
            throw RuntimeException("Process '${args.joinToString(" ")}' failed with exit code ${process.exitValue()} and output: ${process.outputText}")
        }
    } finally {
        process.destroyForcibly()
        process.waitFor()
    }
}

private val Process.outputText: String
    get() {
        InputStreamReader(inputStream, Charsets.UTF_8).use { inputReader ->
            BufferedReader(inputReader).use { bufferedReader ->
                return bufferedReader.readText()
            }
        }
    }
