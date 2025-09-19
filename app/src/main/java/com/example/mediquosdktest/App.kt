package com.example.mediquosdktest

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.mediquo.chat.MediquoAuthenticateListener
import com.mediquo.chat.MediquoDeAuthenticateListener
import com.mediquo.chat.MediquoInitListener
import com.mediquo.chat.MediquoSDK
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App: Application(){

    companion object {
        const val TAG = "MediquoSDKExample"
    }

    private val mediQuoInitListener = object : MediquoInitListener {
        override fun onFailure(message: String?) {
            Log.d(TAG, "Failure initializing MediquoSDK: $message")
        }

        override fun onSuccess() {
            Log.d(TAG, "Initialized MediquoSDK Successfully")
        }
    }

    override fun onCreate() {
        super.onCreate()
        MediquoSDK.initialize(this, getString(R.string.api_key), mediQuoInitListener)
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FCM", "Fetching FCM token failed", task.exception)
                    return@addOnCompleteListener
                }

                // Token obtenido exitosamente
                val token = task.result
                MediquoSDK.getInstance()?.registerPushToken(token)
                Log.d("FCM", "Firebase Token: $token")
            }
    }
}