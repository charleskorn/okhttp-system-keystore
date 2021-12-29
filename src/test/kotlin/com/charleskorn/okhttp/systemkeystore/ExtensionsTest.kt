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
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.net.ssl.SSLHandshakeException

// https://adambennett.dev/2021/09/mockwebserver-https/ is a very useful reference,
// as is https://github.com/square/okhttp/blob/master/okhttp-tls/README.md.
class ExtensionsTest : FunSpec({
    val now = ZonedDateTime.now(ZoneId.of("UTC"))

    val untrustedCertificate = autoClose(CertificateContainer.createUntrusted("Untrusted certificate for okhttp-system-keystore tests running at $now"))
    val untrustedServer = autoClose(createServer(untrustedCertificate.heldCertificate))

    // Important: we must add the certificate to the system keystore before we create the client below (as Java loads the list
    // of certificates from the system when we configure the keystore below).
    val trustedCertificate = autoClose(CertificateContainer.createAndTrustIfSupported("Trusted certificate for okhttp-system-keystore tests running at $now"))
    val trustedServer = autoClose(createServer(trustedCertificate.heldCertificate))

    val client = OkHttpClient.Builder()
        .useOperatingSystemCertificateTrustStore()
        .build()

    fun requestShouldSucceed(url: HttpUrl) {
        val request = Request.Builder()
            .get()
            .url(url)
            .build()

        client.newCall(request).execute().use { response ->
            response.code shouldBe 200
        }
    }

    fun requestShouldFailWithUntrustedCertificateError(url: HttpUrl) {
        val request = Request.Builder()
            .get()
            .url(url)
            .build()

        val exception = shouldThrow<SSLHandshakeException> {
            client.newCall(request).execute()
        }

        exception.message shouldBe "PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target"
    }

    context("connecting to a server that should be trusted by default") {
        test("should be able to make requests") {
            requestShouldSucceed("https://google.com".toHttpUrl())
        }
    }

    context("connecting to a server that presents an untrusted certificate") {
        test("should throw an exception") {
            requestShouldFailWithUntrustedCertificateError(untrustedServer.url("/"))
        }
    }

    context("connecting to a server that presents a self-signed certificate trusted by the system trust store") {
        val url = trustedServer.url("/")

        when (OperatingSystem.current) {
            OperatingSystem.Mac -> {
                test("should be able to make requests") {
                    requestShouldSucceed(url)
                }
            }
            OperatingSystem.Other -> {
                test("should throw an exception") {
                    requestShouldFailWithUntrustedCertificateError(url)
                }
            }
        }
    }

    // TODO: Certificate indirectly trusted in OS trust store
})

private fun createServer(certificate: HeldCertificate): MockWebServer {
    val serverCertificates = HandshakeCertificates.Builder()
        .heldCertificate(certificate)
        .build()

    val server = MockWebServer()
    server.useHttps(serverCertificates.sslSocketFactory(), false)
    server.start()

    server.enqueue(MockResponse().setResponseCode(200))

    return server
}
