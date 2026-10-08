<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/670208fd-fc57-4bfe-8cb8-4e2ea0011fb3

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
7. If you have already published your app in AI Studio, please [request upload key reset](https://support.google.com/googleplay/android-developer/answer/9842756#zippy=%2Crequest-an-upload-key-reset) in Google Play Console.

## GitHub APK build

This repository includes GitHub Actions workflows for tests and a signed release APK. See [GITHUB_APK.md](GITHUB_APK.md).

**For an update installation, do not change `applicationId` and keep using the same signing key.** The original AI Studio build in this project is package `com.aistudio.pooleyar.kxvpmw`, versionCode `1`, versionName `1.0`. The release workflow defaults the next build to versionCode `2`.

If you do not have the signing key used by the already-installed app, a newly generated key will not make an APK install as an update.

