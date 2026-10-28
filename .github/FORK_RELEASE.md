# Fork releases

`.github/workflows/fork-release.yml` checks upstream for stable Fossify Clock releases hourly, merges a newer release into the fork, records the upstream tag, and publishes a signed APK. It also builds on pushes to `main` and can be started with **Actions > Build and publish customized Fossify Clock > Run workflow**. The scheduled sync uses a normal merge. If upstream changes conflict with fork changes, the workflow stops before pushing; resolve the conflict in the fork and run the workflow again.

Before the first release, add these repository Actions secrets:

- `CLOCK_KEYSTORE_BASE64`: base64-encoded fork-specific release keystore
- `CLOCK_KEYSTORE_PASSWORD`: keystore password
- `CLOCK_KEY_ALIAS`: key alias
- `CLOCK_KEY_PASSWORD`: key password

Use the same keystore for every release so Android accepts later APKs as updates. The fork uses package ID `org.fossify.clock.fork`, so it installs beside the official app and has separate app data and alarms. Nothing is migrated automatically. To copy alarms and timers, use the official app's **Export alarms and timers**, then the fork's **Import alarms and timers**; settings and other app data remain separate.
