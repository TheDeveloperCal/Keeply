# Keeply — Aveliq

Development build for the Keeply Android app.

## Build on Linux

This project is configured for:
- Android Gradle Plugin 8.6.1
- Gradle 8.7
- JDK 21 to run Gradle
- Java/Kotlin JVM target 17
- Kotlin compiler in-process execution
- Compile SDK 35

Run:

```bash
./BUILD_LINUX.sh
```

APK output:

`app/build/outputs/apk/debug/app-debug.apk`

## Smart Scan

The current development build includes a live CameraX preview with on-device ML Kit OCR, barcode scanning, object detection, and image labeling. Captured frames are parsed into a review/edit flow before saving.

## Build fix in this package

- Fixed Compose Image invocation in the About screen by using named parameters.
- Added the missing `DisposableEffect` import used by the live scanner lifecycle cleanup.


## Stability note
The live camera analyzer intentionally does not run ML Kit Image Labeling. Product/receipt detection uses CameraX, OCR, barcode scanning, and ML Kit object detection to avoid the native crash observed on the test device.


## Stability change
The live analyzer uses CameraX with on-device text recognition and barcode scanning. ML Kit image labeling and object detection are not executed continuously on live frames; this avoids the native vision-pipeline crash observed on the Samsung test device while preserving the live scanner experience.
