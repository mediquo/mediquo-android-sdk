package com.example.mediquosdktest

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.mediquo.sdk.MediQuo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class App : Application() {

    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var sdk: MediQuo? = null
    private var firebaseToken: String? = null
    private var isFirebaseReady = false
    private val sdkInitializationState =
        MutableStateFlow(DemoSdkInitializationState())

    override fun onCreate() {
        super.onCreate()

        initializeSdkIfNeeded()

        isFirebaseReady =
            FirebaseApp.getApps(this).isNotEmpty() || FirebaseApp.initializeApp(this) != null
        if (!isFirebaseReady) {
            Log.w(TAG, "Firebase could not be initialized. Add google-services.json to app/.")
        } else {
            requestFirebaseToken("application start")
        }
    }

    fun attachSdk(instance: MediQuo) {
        sdk = instance
        sdkInitializationState.value = DemoSdkInitializationState(sdk = instance)
        Log.d(TAG, "MediQuo SDK attached")
        registerPendingPushToken()
        requestFirebaseToken("sdk attached")
    }

    fun currentSdk(): MediQuo? = sdk
    fun sdkInitializationState(): StateFlow<DemoSdkInitializationState> =
        sdkInitializationState.asStateFlow()

    fun initializeSdkIfNeeded(forceRetry: Boolean = false) {
        val currentState = sdkInitializationState.value
        if (!forceRetry && (sdk != null || currentState.isLoading)) return

        sdkInitializationState.value = DemoSdkInitializationState(isLoading = true)
        mainScope.launch {
            runCatching {
                MediQuo.create(
                    context = this@App,
                    apiKey = getString(R.string.api_key).trim(),
                    userId = getString(R.string.demo_user_id).trim()
                )
            }.onSuccess {
                attachSdk(it)
            }.onFailure {
                sdk = null
                sdkInitializationState.value = DemoSdkInitializationState(
                    errorMessage = it.message ?: getString(R.string.unknown_error)
                )
            }
        }
    }

    fun updateFirebaseToken(token: String) {
        if (firebaseToken == token) {
            Log.d(TAG, "Firebase token unchanged")
        }
        firebaseToken = token
        registerPendingPushToken()
        Log.d(TAG, "Firebase token: $token")
    }

    private fun registerPendingPushToken() {
        val currentSdk = sdk ?: return
        val token = firebaseToken ?: return

        ioScope.launch {
            runCatching {
                currentSdk.setPushNotificationToken(MediQuo.NotificationType.Firebase(token))
                Log.d(TAG, "FCM token registered in MediQuo")
            }.onFailure {
                Log.w(TAG, "Registering FCM token in MediQuo failed", it)
            }
        }
    }

    private fun requestFirebaseToken(reason: String) {
        if (!isFirebaseReady) {
            Log.w(TAG, "Skipping FCM token request because Firebase is not ready")
            return
        }

        Log.d(TAG, "Requesting FCM token: $reason")
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM token failed for $reason", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
                if (token.isNullOrBlank()) {
                    Log.w(TAG, "FCM token fetch returned an empty token for $reason")
                    return@addOnCompleteListener
                }

                updateFirebaseToken(token)
            }
    }

    private companion object {
        private const val TAG = "MediquoSDKExample"
    }
}

data class DemoSdkInitializationState(
    val sdk: MediQuo? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
