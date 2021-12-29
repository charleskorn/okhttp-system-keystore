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

internal class TestCertificate(
    val commonName: String,
    val isCertificateAuthority: Boolean = false,
    val signedBy: TestCertificate? = null
) {
    internal val heldCertificate = buildCertificate()

    private fun buildCertificate(): HeldCertificate {
        val builder = HeldCertificate.Builder()
            .addSubjectAlternativeName(localhost)
            .organizationalUnit(TestCertificate::class.java.packageName)
            .commonName(commonName)

        if (isCertificateAuthority) {
            builder.certificateAuthority(0)
        }

        if (signedBy != null) {
            builder.signedBy(signedBy.heldCertificate)
        }

        return builder.build()
    }

    companion object {
        private val localhost = InetAddress.getByName("localhost").canonicalHostName
    }
}