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

### Add dependencies to project level build.gradle
````
allprojects {
    repositories {
        google()
        jcenter()
        mavenCentral()
        maven { url "https://mediquo.jfrog.io/artifactory/android-sdk" }
    }
}
````
### Add dependencies to app level build.gradle
````
implementation 'com.mediquo:mediquo-sdk:[LAST-VERSION]'
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