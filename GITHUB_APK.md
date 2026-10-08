# Build an installable APK with GitHub Actions

## The important Android update rule

An APK can update an existing installation only when **all three** are compatible:

1. The `applicationId` (package name) is exactly the same.
2. The new `versionCode` is higher than the installed app.
3. The new APK is signed with the same signing key as the installed app.

This project currently keeps the AI Studio application ID as its default:
`com.aistudio.pooleyar.kxvpmw`.

If your old installed Pooleyar APK has a different package name, change the GitHub build input/property to that exact ID. If you do not know it, Android Studio can show it in the old project's `app/build.gradle(.kts)` as `applicationId`.

If the old APK was signed with a key you no longer have, a new APK **cannot** be installed as an update. You must recover/reset the signing key as appropriate for how the old app was distributed.

## One-time GitHub setup

Create these repository secrets under **Settings → Secrets and variables → Actions**:

- `KEYSTORE_BASE64` — base64 contents of the same `.jks`/`.keystore` used to sign the installed app
- `STORE_PASSWORD` — keystore password
- `KEY_PASSWORD` — key password
- `KEY_ALIAS` — key alias (this project defaults to `upload` if omitted)

Do **not** commit the keystore or passwords to GitHub.

### Convert an existing keystore to `KEYSTORE_BASE64`

Linux/macOS:

```bash
base64 -w 0 my-upload-key.jks
```

macOS alternative:

```bash
base64 my-upload-key.jks | tr -d '\n'
```

Copy the output into the `KEYSTORE_BASE64` secret.

## Build the APK

1. Push this project to GitHub.
2. Open **Actions**.
3. Select **Build Pooleyar APK**.
4. Click **Run workflow**.
5. Set a `versionCode` higher than the currently installed app. For the first update, `2` is appropriate only if the installed app is `1`.
6. Set `versionName`, e.g. `1.1`.
7. After the workflow succeeds, open the run and download the `pooleyar-...` artifact.
8. Install the resulting `app-release.apk` on the phone.

For every later release, increase `versionCode`: 3, 4, 5, ...

## If Android says “App not installed” / “package conflicts”

The most likely causes are:

- package name differs from the installed version;
- signing key differs;
- versionCode is not higher;
- the old app is a different build variant (debug vs release).

Do **not** uninstall the old app just to make the new APK install if you need its local data. Uninstalling can delete the app's private Room database.
