# Android Studio Setup Guide

## Quick Start

### 1. Open in Android Studio

1. Launch **Android Studio**
2. Click **"Open"** on the welcome screen
3. Navigate to this project folder
4. Click **"OK"**

### 2. Configure SDK

When prompted, install the required Android SDK components:
- Android SDK Platform 34
- Android SDK Build-Tools
- Android SDK Platform-Tools

### 3. Update local.properties

The file `local.properties` needs your Android SDK location:

**Windows:**
```properties
sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
```

**Mac:**
```properties
sdk.dir=/Users/YourUsername/Library/Android/sdk
```

**Linux:**
```properties
sdk.dir=/home/YourUsername/Android/Sdk
```

### 4. Sync Project

Android Studio should automatically sync Gradle. If not:
- Click **File > Sync Project with Gradle Files**

### 5. Run the App

Click the green **Run** button (▶️) or press **Shift + F10**

## Setting Up an Emulator

### Create a Virtual Device

1. Click **Tools > Device Manager**
2. Click **"Create Device"**
3. Select **"Phone" > "Pixel 6"**
4. Click **"Next"**
5. Download the recommended system image (API 34)
6. Click **"Finish"**

### Recommended Emulator Settings

- **Device**: Pixel 6 or Pixel 5
- **System Image**: API 34 (Android 14.0)
- **RAM**: 2048 MB
- **VM Heap**: 512 MB
- **Graphics**: Hardware - GLES 2.0

## Common Issues & Solutions

### Issue 1: "SDK location not found"

**Solution:**
1. Open **File > Project Structure**
2. Go to **SDK Location**
3. Set the Android SDK location
4. Click **Apply** and **OK**

### Issue 2: Gradle sync failed

**Solution:**
1. Check internet connection
2. Go to **File > Invalidate Caches / Restart**
3. Select **"Invalidate and Restart"**
4. Wait for Android Studio to restart and rebuild

### Issue 3: Dependencies not downloading

**Solution:**
1. Check `settings.gradle` has the correct repositories:
```gradle
repositories {
    google()
    mavenCentral()
    maven { url = 'https://jitpack.io' }
}
```
2. Run **Build > Clean Project**
3. Run **Build > Rebuild Project**

### Issue 4: Emulator won't start

**Solution:**
1. Ensure virtualization is enabled in BIOS
2. Windows: Enable Hyper-V or install Intel HAXM
3. Mac: System has native support
4. Try creating a new virtual device

## Building APK

### Debug APK (for testing)

1. Click **Build > Build Bundle(s) / APK(s) > Build APK(s)**
2. Wait for build to complete
3. Click on the notification to locate APK
4. Default location: `app/build/outputs/apk/debug/app-debug.apk`

### Release APK (for distribution)

1. Click **Build > Generate Signed Bundle / APK**
2. Select **APK**
3. Create a new keystore or use existing
4. Fill in the required details
5. Select **release** build variant
6. Click **Finish**

## Project Dependencies

All dependencies are managed in `app/build.gradle`:

```gradle
dependencies {
    // Core Android
    implementation 'androidx.appcompat:appcompat:1.7.1'
    implementation 'com.google.android.material:material:1.13.0'
    
    // Navigation
    implementation 'androidx.fragment:fragment:1.8.7'
    implementation 'androidx.navigation:navigation-fragment:2.8.7'
    
    // Lifecycle & ViewModel
    implementation 'androidx.lifecycle:lifecycle-viewmodel:2.8.7'
    implementation 'androidx.lifecycle:lifecycle-livedata:2.8.7'
    
    // Room Database
    implementation 'androidx.room:room-runtime:2.6.1'
    annotationProcessor 'androidx.room:room-compiler:2.6.1'
    
    // Retrofit & Gson
    implementation 'com.squareup.retrofit2:retrofit:2.11.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.11.0'
    
    // Charts
    implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
    
    // Glide
    implementation 'com.github.bumptech.glide:glide:4.16.0'
}
```

## Gradle Properties

### Minimum Requirements

- **JDK**: Version 11
- **Gradle**: Version 8.13
- **Android Gradle Plugin**: 8.13.2
- **compileSdk**: 34
- **minSdk**: 24
- **targetSdk**: 34

### Enable ViewBinding

ViewBinding is enabled in `app/build.gradle`:

```gradle
android {
    buildFeatures {
        viewBinding = true
    }
}
```

## Testing on Physical Device

### Enable Developer Mode

**Android 12+:**
1. Go to **Settings > About Phone**
2. Tap **"Build Number"** 7 times
3. Go back to **Settings > System**
4. Tap **"Developer Options"**
5. Enable **"USB Debugging"**

**Older Android:**
1. Go to **Settings > About Phone**
2. Tap **"Build Number"** 7 times
3. Go to **Settings > Developer Options**
4. Enable **"USB Debugging"**

### Connect Device

1. Connect your Android device via USB
2. Allow USB debugging on your device
3. Run the app from Android Studio
4. Select your device from the list

## Performance Tips

### Speed up Gradle builds

Add to `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
```

### Allocate more memory to Android Studio

1. Go to **Help > Edit Custom VM Options**
2. Increase Xmx value:
```
-Xmx4096m
```

## File Structure Overview

```
MAD Project/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/myapplication/
│   │       │   ├── model/         # Data classes
│   │       │   ├── ui/fragments/  # UI fragments
│   │       │   ├── adapter/       # RecyclerView adapters
│   │       │   ├── database/      # Room database
│   │       │   ├── util/          # Utility classes
│   │       │   └── MainActivity.java
│   │       └── res/
│   │           ├── layout/        # XML layouts
│   │           ├── values/        # Colors, strings, themes
│   │           ├── drawable/      # Images and shapes
│   │           └── menu/          # Menu resources
│   └── build.gradle              # App dependencies
├── gradle/                       # Gradle wrapper
├── build.gradle                  # Project-level build
├── settings.gradle              # Project settings
└── local.properties             # SDK location

```

## Ready to Code!

Your project is now ready for development. The app contains:
- ✅ 5 fully designed fragments
- ✅ Material Design 3 theme
- ✅ MVVM architecture
- ✅ Sample data generator
- ✅ Database schema
- ✅ Beautiful minimalist UI

Start by running the app and exploring the features!

---

**Need help?** Check the main README.md for detailed documentation.
