# okhttp-system-keystore



## Contributing

This project uses Gradle. 

Run linting and tests with `./gradlew check`.

### macOS-specific notes

The tests need to temporarily add certificates to your local Keychain. Therefore, when the tests run you will need to approve adding each certificate (two in total) by entering your password or using Touch ID. 

### Windows-specific notes

The tests need to temporarily add a certificate trusted at the machine-wide level. Therefore, you must run tests from an elevated (administrator) terminal.

If you are using an elevated terminal and still encounter issues, try disabling the Gradle daemon with `--no-daemon`, for example. `./gradlew --no-daemon check`.
(The Gradle daemon might have started un-elevated, disabling the daemon ensures that it runs with the same level of access as your terminal.)
