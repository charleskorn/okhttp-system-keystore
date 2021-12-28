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

internal interface SelfSignedCertificate : AutoCloseable {
    val certificate: HeldCertificate

    companion object {
        fun createUntrusted(commonName: String): SelfSignedCertificate {
            return UntrustedCertificate(commonName)
        }

        fun createAndTrustIfSupported(commonName: String): SelfSignedCertificate {
            return when (OperatingSystem.current) {
                OperatingSystem.Mac -> TrustedCertificate(commonName)
                OperatingSystem.Other -> UntrustedCertificate(commonName)
            }
        }
    }
}
