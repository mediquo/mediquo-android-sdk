package com.example.mediquosdktest

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.mediquo.sdk.MediQuo
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class App : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var sdk: MediQuo? = null
    private var firebaseToken: String? = null

    override fun onCreate() {
        super.onCreate()

        val firebaseReady =
            FirebaseApp.getApps(this).isNotEmpty() || FirebaseApp.initializeApp(this) != null
        if (!firebaseReady) {
            Log.w(TAG, "Firebase could not be initialized. Add google-services.json to app/.")
            return
        }

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM token failed", task.exception)
                    return@addOnCompleteListener
                }

                updateFirebaseToken(task.result)
            }
    }

    fun attachSdk(instance: MediQuo) {
        sdk = instance
        registerPendingPushToken()
    }

    fun updateFirebaseToken(token: String) {
        firebaseToken = token
        registerPendingPushToken()
        Log.d(TAG, "Firebase token: $token")
    }

    private fun registerPendingPushToken() {
        val currentSdk = sdk ?: return
        val token = firebaseToken ?: return

        applicationScope.launch {
            runCatching {
                currentSdk.setPushNotificationToken(MediQuo.NotificationType.Firebase(token))
            }.onFailure {
                Log.w(TAG, "Registering FCM token in MediQuo failed", it)
            }
        }
    }

    private companion object {
        private const val TAG = "MediquoSDKExample"
    }
}
