# MediQuo Android SDK

Welcome to MediQuo Android SDK, the easiest way to integrate MediQuo functionality into your Android app.

This repository includes a sample app you can inspect to see a complete integration of the SDK.

## Prerequisites

- Android Studio Meerkat or later
- Android Gradle Plugin `8.9.1` or later
- Gradle `9.1` or later
- Kotlin `2.2.0`
- `minSdk = 29`
- `compileSdk = 36`
- `targetSdk = 36` recommended
- Jetpack Compose enabled in your app
- Firebase Cloud Messaging if you want push notifications

## Installation

The SDK is distributed as an Android library artifact.

### 1. Add the MediQuo Maven repository

In your root `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://mediquo.jfrog.io/artifactory/android-sdk") }
        maven { url = uri("https://mediquo.jfrog.io/artifactory/videocall-android") }
    }
}
```

### 2. Configure your version catalog

Example `gradle/libs.versions.toml` entries:

```toml
[versions]
mediquoSdk = "[LAST_VERSION]"
kotlin = "2.2.0"

[libraries]
mediquo-sdk = { group = "com.mediquo", name = "mediquo-sdk", version.ref = "mediquoSdk" }

[plugins]
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

### 3. Add plugins and dependencies to your app module

In `app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.yourapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.yourapp"
        minSdk = 29
        targetSdk = 36
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.mediquo.sdk)
}
```

### 4. Add the Google Services plugin at project level

If you use Firebase, make sure the plugin is available in the root build:

```kotlin
plugins {
    id("com.google.gms.google-services") version "4.4.2" apply false
}
```

## Integration

We recommend creating the SDK right after an entitled user logs in.

Before doing that, make sure you have these two values:

- `API_KEY`: provided by mediQuo
- `USER_ID`: the MediQuo patient identifier associated with the logged-in user

### 1. Create a single SDK instance per user session

```kotlin
import com.mediquo.sdk.MediQuo

suspend fun createMediQuo(context: Context): MediQuo {
    return MediQuo.create(
        context = context,
        apiKey = API_KEY,
        userId = USER_ID
    )
}
```

For most applications, we recommend instantiating a single `MediQuo` object at the start of the user session and storing it in your dependency container, `ViewModel`, or session manager.

### 2. Clean up the SDK on logout

When the user logs out, make sure you deauthenticate the SDK:

```kotlin
try {
    mediquo.deauthenticateSDK()
} catch (t: Throwable) {
    // Handle error if needed
}
```

This ensures the next user starts from a clean state.

## Rendering SDK screens

The Android SDK supports three integration styles:

- Compose with `sdkView(...)`
- Launching a dedicated Activity with `createIntent(...)`
- Embedding in a `Fragment` with `createFragment(...)`

All available destinations are defined in `MediQuo.ViewKind`.

### Option A. Compose integration

If your app already uses Compose, this is the most direct integration path.

```kotlin
@Composable
fun MediquoScreen(mediquo: MediQuo) {
    mediquo.sdkView(
        kind = MediQuo.ViewKind.ProfessionalList,
        modifier = Modifier.fillMaxSize()
    )
}
```

You can also handle the close event when the SDK screen is used as a modal flow:

```kotlin
mediquo.sdkView(
    kind = MediQuo.ViewKind.ProfessionalList,
    modifier = Modifier.fillMaxSize(),
    onClose = { /* navigate back */ }
)
```

### Option B. Launch in a dedicated Activity

If your app is View-based or you prefer a separate screen:

```kotlin
val intent = mediquo.createIntent(
    context = this,
    kind = MediQuo.ViewKind.ProfessionalList()
)
startActivity(intent)
```

### Option C. Embed in a Fragment

If you want to host the SDK inside an existing Fragment-based screen:

```kotlin
val fragment = mediquo.createFragment(MediQuo.ViewKind.ProfessionalList())

supportFragmentManager.beginTransaction()
    .replace(R.id.container, fragment)
    .addToBackStack(null)
    .commit()
```

## Available views

These are the main entry points you can open through `MediQuo.ViewKind`:

```kotlin
MediQuo.ViewKind.ProfessionalList()
MediQuo.ViewKind.AppointmentsDetails(appointmentId = "123")
MediQuo.ViewKind.Chat(roomId = 123)
MediQuo.ViewKind.MedicalHistory
MediQuo.ViewKind.Allergies
MediQuo.ViewKind.Diseases
MediQuo.ViewKind.MedicalReport
MediQuo.ViewKind.Medication
MediQuo.ViewKind.Prescription
MediQuo.ViewKind.Documentation
MediQuo.ViewKind.Call(callViewModel = ...)
```

## Support button on Professional List

`ProfessionalList` supports an optional support CTA, similar to iOS:

```kotlin
mediquo.sdkView(
    kind = MediQuo.ViewKind.ProfessionalList(
        supportButton = MediQuo.SupportButtonConfiguration(
            title = "Support",
            onTap = {
                // Open your support flow
            }
        )
    ),
    modifier = Modifier.fillMaxSize()
)
```

You can also customize the button icon and background color:

```kotlin
MediQuo.SupportButtonConfiguration(
    title = "Support",
    icon = myImageVector,
    backgroundColor = Color.Red,
    onTap = { /* ... */ }
)
```

## Push notifications

The SDK supports Firebase push token registration through:

```kotlin
mediquo.setPushNotificationToken(MediQuo.NotificationType.Firebase(token))
```

### 1. Add Firebase to your project

- Add `google-services.json` to your app module
- Configure Firebase Messaging in your project
- Apply `com.google.gms.google-services`

You can follow the official Firebase guide here:
[Firebase Cloud Messaging for Android](https://firebase.google.com/docs/cloud-messaging/android/client)

### 2. Initialize Firebase and obtain the FCM token

Example in your `Application`:

```kotlin
class App : Application() {
    private var mediquo: MediQuo? = null
    private var firebaseToken: String? = null

    override fun onCreate() {
        super.onCreate()

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) return@addOnCompleteListener
                firebaseToken = task.result
                registerPendingPushToken()
            }
    }

    fun attachMediQuo(instance: MediQuo) {
        mediquo = instance
        registerPendingPushToken()
    }

    fun updateFirebaseToken(token: String) {
        firebaseToken = token
        registerPendingPushToken()
    }

    private fun registerPendingPushToken() {
        val sdk = mediquo ?: return
        val token = firebaseToken ?: return

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            sdk.setPushNotificationToken(MediQuo.NotificationType.Firebase(token))
        }
    }
}
```

### 3. Refresh the token when Firebase rotates it

```kotlin
class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        (application as? App)?.updateFirebaseToken(token)
    }
}
```

### 4. Add the service to your manifest

```xml
<service
    android:name=".MyFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

### 5. Request notification permission on Android 13+

```kotlin
private fun askForNotificationPermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }
    }
}
```

### 6. Handle incoming push payloads

The SDK does not automatically decide how your app should navigate when a push is tapped. Your app owns that behavior.

The recommended pattern is:

- Receive the FCM message in your `FirebaseMessagingService`
- Show your notification
- Forward the push extras to your launcher Activity
- Parse those extras in your app and decide whether to open a chat, appointment, or incoming call UI

For incoming calls, the sample app maps push extras like:

- `type`
- `call_uuid`
- `call_room_id`
- `call_session_id`
- `call_type`
- `call_token`
- `professional_hash`
- `professional_name`
- `image`

and builds a `MediQuo.CallViewModel` to open:

```kotlin
mediquo.sdkView(
    kind = MediQuo.ViewKind.Call(callViewModel = callViewModel),
    modifier = Modifier.fillMaxSize()
)
```

## Listening to events

To listen to SDK socket events and incoming calls, assign a `MediQuoEventDelegate`:

```kotlin
val delegate = object : MediQuoEventDelegate {
    override suspend fun didChangeSocketStatus(
        isConnected: Boolean,
        previousIsConnected: Boolean
    ) {
        Log.d("MediQuo", "Socket status: $previousIsConnected -> $isConnected")
    }

    override suspend fun didReceiveCall(call: MediQuo.CallViewModel) {
        // Present call UI
    }

    override suspend fun didRejectCall(callId: String) {
        // Dismiss current call UI if needed
    }
}

mediquo.eventDelegate = delegate
```

Use this delegate if you want to:

- react to WebSocket connection changes
- present your own incoming call UI
- dismiss call UI when the remote call is rejected

## Android permissions

Depending on the features you enable, the SDK may require:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

If your app supports videocalls, also declare the camera feature:

```xml
<uses-feature
    android:name="android.hardware.camera"
    android:required="false" />
```

## File attachments

To support attachments correctly on Android 11+, add a `FileProvider`.

### 1. Create `res/xml/file_paths.xml`

Replace `your.package.name` with your real application id:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <external-path name="my_images" path="Android/data/your.package.name/files/Pictures" />
    <external-path name="downloads" path="Android/data/your.package.name/files/Download" />
</paths>
```

### 2. Register the provider in `AndroidManifest.xml`

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

## Customization

The SDK uses the host app theme and its own internal design tokens. The most common customization point exposed to integrators today is the optional support button in `ProfessionalList`.

If you need deeper theming or UI customization, please contact mediQuo or open an issue in the repository.

## Troubleshooting

### Build fails because of outdated Android toolchain

If Gradle reports that dependencies require newer Android APIs or a newer Android Gradle Plugin, make sure your app uses:

- `compileSdk = 36`
- Android Gradle Plugin `8.9.1` or newer
- Kotlin `2.2.0`

### Push token is never registered

Check the following:

- `google-services.json` is present in the app module
- Firebase initializes successfully
- you call `setPushNotificationToken(...)` after both the SDK instance and the FCM token are available
- your `FirebaseMessagingService.onNewToken(...)` updates the stored token

### Incoming calls are not shown

Check the following:

- your app sets `mediquo.eventDelegate`
- your app handles `didReceiveCall(...)`
- if the call comes from a push tap, your launcher Activity reads the push extras and transforms them into a `MediQuo.CallViewModel`

## Sample app

You can inspect the sample app in this repository for a working end-to-end reference, including:

- SDK initialization
- full demo navigation
- push token registration
- incoming call handling

## Need help?

If you need any extra integration point or customization capability, please contact mediQuo.
