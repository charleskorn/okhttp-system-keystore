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

internal enum class OperatingSystem {
    Mac,
    Windows,
    Other;

    companion object {
        internal val current: OperatingSystem = determineCurrentOperatingSystem()

        private fun determineCurrentOperatingSystem(): OperatingSystem {
            val osName = System.getProperty("os.name")

            return when {
                osName.startsWith("mac", ignoreCase = true) -> Mac
                osName.startsWith("windows", ignoreCase = true) -> Windows
                else -> Other
            }
        }
    }
}
