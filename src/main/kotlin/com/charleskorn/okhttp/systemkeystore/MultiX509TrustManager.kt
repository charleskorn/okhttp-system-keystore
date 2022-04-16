/*
    Copyright 2017-2021 Charles Korn.

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

import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

internal class MultiX509TrustManager(private val managers: List<X509TrustManager>) : X509TrustManager {
    init {
        if (managers.isEmpty()) {
            throw IllegalArgumentException("Must provide at least one trust manager.")
        }
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        throw UnsupportedOperationException()
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        managers.forEachIndexed { index, manager ->
            try {
                manager.checkServerTrusted(chain, authType)

                return
            } catch (e: CertificateException) {
                if (index == managers.size - 1) {
                    // We've exhausted all possible trust managers, give up.
                    throw e
                }
            }
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return managers.flatMap { it.acceptedIssuers.toList() }.toTypedArray()
    }
}
