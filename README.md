# MediQuo Android SDK
Welcome to MediQuo Android SDK, the easiest way yo integrate the MediQuo functionality into your app!

This repository contains a sample app that you can inspect to see how to integrate the MediQuo SDK into your own Application.

## Prerequisites
- minSdkVersion 26
- recommended targetSdk 35
- recommended compileSdkVersion 35
- Android Studio Ladybug or later
- Gradle version 8.7
- Kotlin version 1.9.24
- Compose BOM version 2024.06.00
- Incluse compose material 3, version provided by compose BOM, follow this [instructions](https://developer.android.com/develop/ui/compose/setup#setup-compose)
- Firebase Cloud Messaging, Firebase version provided by Firebase BOM version 32.1.1, follow this [instructions](https://firebase.google.com/docs/cloud-messaging/android/client)
- compileOptions set to Java 8

## Usage

### Add mandatory dependencies to project level build.gradle
````
plugins {
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
}

allprojects {
	dependencies {
        classpath("com.google.gms:google-services:4.3.15")
    }

    repositories {
        google()
        jcenter()
        mavenCentral()
        maven { url "https://mediquo.jfrog.io/artifactory/android-sdk" }
    }
}
````

### Add mandatory dependencies to app level build.gradle
````
plugins {
	alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
	}
	kotlinOptions {
		 jvmTarget = "17"
	}
}

dependencies {
	implementation(libs.hiltAndroid)
    ksp(libs.hiltCompiler)

	implementation(libs.mediquo.sdk)
}
````
### Configure libs.versions.toml
````
[versions]
mediquoSdk = "[LAST-VERSION]"
hiltVersion = "2.54"
kspVersion = "1.9.0-1.0.13"

[libraries]
hiltAndroid = { group = "com.google.dagger", name = "hilt-android", version.ref = "hiltVersion" }
hiltCompiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hiltVersion" }
mediquo-sdk = { group = "com.mediquo", name = "mediquo-sdk", version.ref = "mediquoSdk" }

[plugins]
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hiltVersion" }
ksp = { id = "com.google.devtools.ksp", version.ref = "kspVersion" }

````

### Include file_paths.xml file
Since Android 11 you must add a file named file_paths.xml in the res/xml directory of your app module for the file attachment to work properly.

Inside this file, you have to add the following code (make sure to replace your.package.name with the real value):
````
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <external-path name="my_images" path="Android/data/[your.package.name]/files/Pictures" />
    <external-path name="downloads" path="Android/data/[your.package.name]/files/Download" />
</paths>
````
On the other hand, for its correct operation, we must refer to this file in AndroidManifest.xml, addind the following code:
````
 <provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="[your.package.name]"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
````
### Initialize SDK
The library must be initialized inside Application.onCreate() using your API_KEY provided by mediQuo. Make sure not to use any other library method before you receive a successful response in the listener.
````kotlin
class App : Application() {
    private val mediQuoInitListener = object : MediquoInitListener {
        override fun onFailure(message: String?) {
            /* Your initialization has failed */
        }

        override fun onSuccess() {
            /* Your initialization has been successful */
        }
    }

    override fun onCreate() {
        super.onCreate()
        MediquoSDK.initialize(this, API_KEY, mediQuoInitListener)
    }
 }
````
### Authenticate
````kotlin
private val mediQuoAuthenticateListener = object : MediquoAuthenticateListener {
    override fun onFailure(message: String?) {
        /* Your authentication has failed */
    }

    override fun onSuccess() {
        /* Your authentication has been successful */
    }
}

private fun authenticateMediQuoSDK() {
    MediquoSDK.authenticate(CLIENT_CODE, mediQuoAuthenticateListener)
}
````
### Logout
````kotlin
private val mediquoDeAuthenticateListener = object : MediquoDeAuthenticateListener {
    override fun onSuccess() {
        /* Your logout has been successful */
    }

    override fun onFailure(message: String?) {
        /* Your logout has failed */
    }
}

private fun authenticateMediQuoSDK() {
    MediquoSDK.deAuthenticate(mediquoDeAuthenticateListener)
}
````
## Push notifications

In order to receive Push Notifications in your app, make sure that:
- The `google-services.json` is added to your project. 
- In the App's `onCreate`, make sure you init Firebase and send to `MediquoSDK` the App's token:

```kotlin
if (FirebaseApp.getApps(this).isEmpty()) {
    FirebaseApp.initializeApp(this)
}

FirebaseMessaging.getInstance().token
    .addOnCompleteListener { task ->
        if (!task.isSuccessful) {
            Log.w("FCM", "Fetching FCM token failed", task.exception)
            return@addOnCompleteListener
        }
        val token = task.result
        MediquoSDK.getInstance()?.registerPushToken(token)
        Log.d("FCM", "Firebase Token: $token")
    }
```

- In the App's Manifest, please add the following snippet:

```xml
<service android:name="com.mediquo.chat.fcm.MediquoFirebaseMessagingService" android:exported="false">
	<intent-filter>
		<action android:name="com.google.firebase.MESSAGING_EVENT" />
	</intent-filter>
</service>
```

## Proguard rules

````
-keep class com.opentok.** { *; }
-keep class org.webrtc.** { *; }
-keep class com.mediquo.ophiuchus.videocall.** { *; }
-keep class org.otwebrtc.** { *; }
-dontwarn com.opentok.**
-dontwarn org.webrtc.**
-dontwarn com.mediquo.ophiuchus.videocall.**

-dontwarn com.google.api.client.http.GenericUrl
-dontwarn com.google.api.client.http.HttpHeaders
-dontwarn com.google.api.client.http.HttpRequest
-dontwarn com.google.api.client.http.HttpRequestFactory
-dontwarn com.google.api.client.http.HttpResponse
-dontwarn com.google.api.client.http.HttpTransport
-dontwarn com.google.api.client.http.javanet.NetHttpTransport$Builder
-dontwarn com.google.api.client.http.javanet.NetHttpTransport
-dontwarn org.joda.time.Instant
````

## Customization

In order to assign new values ​​to change the application's color styles, you will need to declare some variables in your `colors.xml` file to do that:

### Main colors

````
<color name="mediquo_primary_color">#000000</color>
<color name="mediquo_secondary_color">#000000</color>
<color name="mediquo_accent_color">#000000</color>
<color name="mediquo_primary_contrast_color">#000000</color>
<color name="mediquo_notification_color">#000000</color>
````

The function of each variable is as follows:

`mediquo_primary_color` -> Toolbars background && Medical history icons color  
`mediquo_primary_contrast_color` -> Toolbars text and back icon  
`mediquo_secondary_color` -> Speciality label on ProfessionalListFragment && Background professional description on Professional profile  
`mediquo_accent_color` -> Unread messages badge && Lock icon on Professional list  
`mediquo_notification_color` -> Accent color to use on push notifications    

### Chat colors

````
<color name="mediquo_message_text_color_mine">#000000</color>
<color name="mediquo_message_background_color_mine">#000000</color>
<color name="mediquo_message_text_color_their">#000000</color>
<color name="mediquo_message_background_color_their">#000000</color>
<color name="mediquo_message_text_color_alert">#000000</color>
<color name="mediquo_message_background_color_alert">#000000</color>
````

The function of each variable is as follows:

`mediquo_message_text_color_mine` -> Text color of your own message  
`mediquo_message_background_color_mine` -> Background color of your own message  
`mediquo_message_text_color_their` -> Text color of a foreign message  
`mediquo_message_background_color_their` -> Background color of a foreign message  
`mediquo_message_text_color_alert` -> Text color of a alert message  
`mediquo_message_background_color_alert` -> Background color of a alert message

### Font customization
On the same way, if you want to customize the `font` style of the app you will have to declare some new files

First of all, you'll have to create a **New folder** for this files, and to do that you'll have to do **Right Click** on `res` folder and select `New > Android resource directory`, in the Resource type list you'll have to select `Font` and then click `OK`

Now you'll have to create three files named **mediquo_bold**, **mediquo_medium** and **mediquo_regular** to that directory, these files should are `otf` or `ttf` type.

### Support Button Config 
[min version](https://github.com/mediquo/mediquo-android-sdk/releases/tag/3.5.2)

New API for SDK integrators to add a button for custom support on the Professional List screen:

```kotlin
try {
  val listener = BaseActivity.OnClickListener {
      Toast.makeText(context, "Support button clicked!", Toast.LENGTH_SHORT).show()
  }
   val buttonConfig = SupportButtonConfiguration(title = "Support Button", image = R.drawable.ic_example, backgroundColor = Color.Red)
   mediquoSDK.openProfessionalListActivity(this@MainActivity, listener, buttonConfig)
} catch (e: Throwable) {
   Log.d("Throwable-exception", e.message.toString())
}
````

For the new call to navigate to the ProfessionalList you will need two things, the **listener** to retrieve the callBack of the button, and a **SupportButtonConfiguration** to specify the `title`, `image` **(as a Drawable resource)** or `background` of the new button.

And this will result in this button being rendered:

![Captura de pantalla 2025-05-29 a las 11 34 12](https://github.com/user-attachments/assets/906e2cd8-66b5-45b1-9a71-7662a5af324b)

Otherwise, if you don't want the new button to be added, you can call the navigation as always:

`mediquoSDK.openProfessionalListActivity(this@MainActivity)
