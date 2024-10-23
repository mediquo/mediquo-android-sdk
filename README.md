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
````
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
````
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
````
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

````
class MediQuoSDKExampleMessagingService : FirebaseMessagingService() {
   override fun onMessageReceived(remoteMessage: RemoteMessage) {
       super.onMessageReceived(remoteMessage)
        
       /* Your code to process remoteMessage */
       
       /* Send remoteMessage to mediQuo SDK */
       MediquoSDK.getInstance()?.onFirebaseMessageReceived(remoteMessage)
   }

   override fun onNewToken(newToken: String) {
       super.onNewToken(newToken)
       /* Register push token to mediQuo SDK */
       MediquoSDK.getInstance()?.registerPushToken(newToken)
   }
}
````

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
