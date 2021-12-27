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

import okhttp3.OkHttpClient
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

public fun OkHttpClient.Builder.useOperatingSystemCertificateTrustStore(): OkHttpClient.Builder {
    val trustManagers = listOf(getDefaultTrustManager()) + getOSTrustManagers()
    val trustManager = MultiX509TrustManager(trustManagers)

    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, arrayOf<TrustManager>(trustManager), null)

    return this.sslSocketFactory(sslContext.socketFactory, trustManager)
}

private fun getDefaultTrustManager(): X509TrustManager {
    val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    factory.init(null as KeyStore?)

    return factory.trustManagers.single() as X509TrustManager
}

private fun getOSTrustManagers(): List<X509TrustManager> {
    val osName = System.getProperty("os.name")

    return when {
        osName.startsWith("mac", ignoreCase = true) -> listOf(getMacTrustManager())
        else -> emptyList()
    }
}

private fun getMacTrustManager(): X509TrustManager {
    val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    val osKeyStore = KeyStore.getInstance("KeychainStore")
    osKeyStore.load(null, null)
    factory.init(osKeyStore)

    return factory.trustManagers.single() as X509TrustManager
}
