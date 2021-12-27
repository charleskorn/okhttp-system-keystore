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

package com.charleskorn.okhttp.systemkeystore

import okhttp3.tls.HeldCertificate
import java.net.InetAddress
import java.nio.file.Files
import java.nio.file.Path

internal class TrustedCertificate(private val commonName: String) : AutoCloseable {
    private val localhost = InetAddress.getByName("localhost").canonicalHostName

    val certificate = HeldCertificate.Builder()
        .addSubjectAlternativeName(localhost)
        .organizationalUnit(TrustedCertificate::class.java.packageName)
        .commonName(commonName)
        .build()

    private val certificatePath = writeToTemporaryFile()

    init {
        addToLocalTrustStore()
    }

    private fun writeToTemporaryFile(): Path {
        val path = Files.createTempFile("okhttp-systemkeystore-$commonName", ".pem")

        Files.write(path, certificate.certificatePem().toByteArray(Charsets.UTF_8))

        return path
    }

    private fun removeCertificateFile() {
        Files.delete(certificatePath)
    }

    private fun addToLocalTrustStore() {
        runProcess("security", "add-trusted-cert", "-p", "ssl", "-r", "trustRoot", "-k", userKeychainPath, certificatePath.toString())
    }

    private fun removeFromLocalTrustStore() {
        runProcess("security", "remove-trusted-cert", certificatePath.toString())
        runProcess("security", "delete-certificate", "-c", commonName, userKeychainPath)
    }

    private fun runProcess(vararg args: String) {
        val process = ProcessBuilder()
            .command(*args)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .redirectErrorStream(true)
            .start()

        val exitCode = process.waitFor()

        if (exitCode != 0) {
            val output = process.inputReader(Charsets.UTF_8).readText()

            throw RuntimeException("Process '${args.joinToString(" ")}' failed with exit code $exitCode and output: $output")
        }
    }

    override fun close() {
        removeFromLocalTrustStore()
        removeCertificateFile()
    }

    companion object {
        private val userKeychainPath = System.getProperty("user.home")!! + "/Library/Keychains/login.keychain-db"
    }
}
