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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import java.net.InetAddress
import javax.net.ssl.SSLHandshakeException

// https://adambennett.dev/2021/09/mockwebserver-https/ is a very useful reference,
// as is https://github.com/square/okhttp/blob/master/okhttp-tls/README.md.
class ExtensionsTest : FunSpec({
    val client = OkHttpClient.Builder()
        .useOperatingSystemCertificateTrustStore()
        .build()

    fun createUntrustedServer(): MockWebServer {
        val localhost = InetAddress.getByName("localhost").canonicalHostName
        val certificate = HeldCertificate.Builder()
            .addSubjectAlternativeName(localhost)
            .build()

        val serverCertificates = HandshakeCertificates.Builder()
            .heldCertificate(certificate)
            .build()

        val server = MockWebServer()
        server.useHttps(serverCertificates.sslSocketFactory(), false)
        server.start()

        return server
    }

    val untrustedServer = autoClose(createUntrustedServer())

    context("connecting to a server that should be trusted by default") {
        test("should be able to make requests") {
            val request = Request.Builder().get().url("https://google.com").build()

            client.newCall(request).execute().use { response ->
                response.code shouldBe 200
            }
        }
    }

    context("connecting to a server that presents an untrusted certificate") {
        test("should throw an exception") {
            val request = Request.Builder()
                .get()
                .url(untrustedServer.url("/"))
                .build()

            val exception = shouldThrow<SSLHandshakeException> {
                client.newCall(request).execute()
            }

            exception.message shouldBe "PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target"
        }
    }
})
