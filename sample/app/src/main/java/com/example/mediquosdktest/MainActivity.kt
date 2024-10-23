package com.example.mediquosdktest

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.example.mediquosdktest.App.Companion.TAG
import com.example.mediquosdktest.ui.LoginButton
import com.example.mediquosdktest.ui.NavigateButton
import com.google.firebase.FirebaseApp
import com.mediquo.chat.MediquoAuthenticateListener
import com.mediquo.chat.MediquoDeAuthenticateListener
import com.mediquo.chat.MediquoSDK

class MainActivity : AppCompatActivity() {
    private val mediquoSDK: MediquoSDK by lazy { MediquoSDK.getInstance() }

    private val mediQuoAuthenticateListener = object : MediquoAuthenticateListener {
        override fun onFailure(message: String?) {
            Log.d(TAG, "Failure authenticating MediquoSDK, $message")
        }

        override fun onSuccess() {
            Log.d(TAG, "Authenticated MediquoSDK Successfully")
        }
    }

    private val mediquoDeAuthenticateListener = object : MediquoDeAuthenticateListener {
        override fun onSuccess() {
            Log.d(TAG, "Deauthenticated MediquoSDK Successfully")
        }

        override fun onFailure(message: String?) {
            Log.d(TAG, "Failure deauthenticating MediquoSDK, $message")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }

        val composeView: ComposeView = findViewById(R.id.compose_view)

        // Set the content for the ComposeView
        composeView.setContent {
            val loggedIn = remember { mutableStateOf(MediquoSDK.getInstance().isAuthenticated == true) }
            Column (modifier = Modifier.fillMaxSize()){
                LoginButton(
                    isLogged = loggedIn.value,
                    onClick = {
                        if(FirebaseApp.getApps(this@MainActivity).isEmpty() == false && MediquoSDK.getInstance().isAuthenticated != true){
                            MediquoSDK.authenticateWithToken(getString(R.string.access_token))
                            Toast.makeText(this@MainActivity, "Logged in", Toast.LENGTH_SHORT).show()
                            loggedIn.value = true
                        }
                    })
                NavigateButton(
                    onClick = {
                        if(loggedIn.value){
                            mediquoSDK.openProfessionalListActivity(this@MainActivity)
                        }else{
                            Toast.makeText(this@MainActivity, "First you have to log in", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
        }

    }
}
