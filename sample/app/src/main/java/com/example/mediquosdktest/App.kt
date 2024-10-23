package com.example.mediquosdktest

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.mediquo.chat.MediquoAuthenticateListener
import com.mediquo.chat.MediquoDeAuthenticateListener
import com.mediquo.chat.MediquoInitListener
import com.mediquo.chat.MediquoSDK

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
    }
}