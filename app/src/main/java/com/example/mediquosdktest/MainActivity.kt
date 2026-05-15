package com.example.mediquosdktest

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mediquo.sdk.MediQuo
import com.mediquo.sdk.MediQuoEventDelegate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.net.URI

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DemoIncomingCallStore.consumeFromIntent(intent)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SDKDemoApp(onAskNotificationPermissions = ::askForNotificationPermissions)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        DemoIncomingCallStore.consumeFromIntent(intent)
    }

    private fun askForNotificationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATION
                )
            }
        }
    }

    private companion object {
        private const val REQUEST_CODE_NOTIFICATION = 1001
    }
}

@Composable
private fun SDKDemoApp(
    onAskNotificationPermissions: () -> Unit
) {
    val context = LocalContext.current
    val key = stringResource(R.string.api_key)
    var apiKey by rememberSaveable { mutableStateOf(key) }
    var userId by rememberSaveable { mutableStateOf(context.getString(R.string.demo_user_id)) }
    var appointmentId by rememberSaveable { mutableStateOf("") }
    var roomId by rememberSaveable { mutableStateOf("") }
    var sdk by remember { mutableStateOf<MediQuo?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentDemo by remember { mutableStateOf<DemoDestination?>(null) }
    val incomingCallViewModel by DemoIncomingCallStore.incomingCallViewModel.collectAsState()
    val scope = rememberCoroutineScope()
    val eventDelegate = remember {
        object : MediQuoEventDelegate {
            override suspend fun didChangeSocketStatus(
                isConnected: Boolean,
                previousIsConnected: Boolean
            ) {
                Log.d("SDKDemoWS", "status: $previousIsConnected -> $isConnected")
            }

            override suspend fun didReceiveCall(call: MediQuo.CallViewModel) {
                DemoIncomingCallStore.showIncomingFromSocket(call)
            }

            override suspend fun didRejectCall(callId: String) {
                DemoIncomingCallStore.clearIncomingCallIfMatches(callId)
            }
        }
    }

    fun loadSdk() {
        errorMessage = null
        isLoading = true
        scope.launch {
            runCatching {
                MediQuo.create(
                    context = context,
                    apiKey = apiKey.trim(),
                    userId = userId.trim()
                )
            }.onSuccess {
                it.eventDelegate = eventDelegate
                sdk = it
                (context.applicationContext as? App)?.attachSdk(it)
                onAskNotificationPermissions()
            }.onFailure {
                sdk = null
                errorMessage = it.message ?: context.getString(R.string.unknown_error)
            }
            isLoading = false
        }
    }

    val activeSdk = sdk
    val activeDemo = currentDemo
    if (activeSdk != null && incomingCallViewModel != null) {
        BackHandler(enabled = false) {}

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                activeSdk.sdkView(
                    kind = MediQuo.ViewKind.Call(
                        callViewModel = incomingCallViewModel!!,
                        closeHandler = { DemoIncomingCallStore.clearIncomingCall() }
                    ),
                    modifier = Modifier.fillMaxSize(),
                    onClose = { DemoIncomingCallStore.clearIncomingCall() }
                )
            }
        }
        return
    }

    if (activeSdk != null && activeDemo != null) {
        val viewKind = activeDemo.toViewKind(
            appointmentId = appointmentId,
            roomId = roomId,
            onSupportTapped = {
                Toast.makeText(context, context.getString(R.string.support_tapped), Toast.LENGTH_SHORT)
                    .show()
            },
            onClose = { currentDemo = null }
        )

        if (viewKind == null) {
            currentDemo = null
            errorMessage = when (activeDemo) {
                DemoDestination.AppointmentDetails -> context.getString(R.string.invalid_appointment_id)
                DemoDestination.Chat -> context.getString(R.string.invalid_room_id)
                else -> context.getString(R.string.unavailable_demo)
            }
            return
        }

        BackHandler {
            currentDemo = null
        }

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                activeSdk.sdkView(
                    kind = viewKind,
                    modifier = Modifier.fillMaxSize(),
                    onClose = { currentDemo = null }
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.demo_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(R.string.demo_subtitle_full),
            style = MaterialTheme.typography.bodyLarge
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text(stringResource(R.string.api_key_label)) },
            singleLine = true
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = userId,
            onValueChange = { userId = it },
            label = { Text(stringResource(R.string.user_id_label)) },
            singleLine = true
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && apiKey.isNotBlank() && userId.isNotBlank(),
            onClick = ::loadSdk
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Text(
                    if (sdk == null) {
                        stringResource(R.string.initialize_sdk)
                    } else {
                        stringResource(R.string.reconnect_sdk)
                    }
                )
            }
        }

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (activeSdk != null) {
            Text(
                text = stringResource(R.string.views),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            DemoDestination.entries.forEach { destination ->
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { currentDemo = destination }
                ) {
                    Text(stringResource(destination.titleRes))
                }
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = appointmentId,
                onValueChange = { appointmentId = it },
                label = { Text(stringResource(R.string.appointment_id_label)) },
                singleLine = true
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = roomId,
                onValueChange = { roomId = it },
                label = { Text(stringResource(R.string.room_id_label)) },
                singleLine = true
            )
        }
    }
}

private object DemoIncomingCallStore {
    private val _incomingCallViewModel = MutableStateFlow<MediQuo.CallViewModel?>(null)
    val incomingCallViewModel = _incomingCallViewModel

    private fun hasActiveIncomingCall(): Boolean = _incomingCallViewModel.value != null

    fun consumeFromIntent(intent: Intent?) {
        if (intent == null) return

        val type = intent.getStringExtra("type")
        if (type != "call_requested" || hasActiveIncomingCall()) return

        val callUuid = intent.getStringExtra("call_uuid").orEmpty()
        val callRoomId = intent.getStringExtra("call_room_id")?.toIntOrNull() ?: return
        val callSessionId = intent.getStringExtra("call_session_id").orEmpty()
        val callType = intent.getStringExtra("call_type")
        val callToken = intent.getStringExtra("call_token").orEmpty()
        val professionalHash = intent.getStringExtra("professional_hash").orEmpty()
        val professionalName = intent.getStringExtra("professional_name").orEmpty()
        val professionalAvatar = intent.getStringExtra("image")

        _incomingCallViewModel.value = MediQuo.CallViewModel(
            id = callUuid,
            roomId = callRoomId,
            sessionId = callSessionId,
            tokenId = callToken,
            type = if (callType.equals("video", ignoreCase = true)) {
                MediQuo.CallViewModel.CallType.VIDEO
            } else {
                MediQuo.CallViewModel.CallType.AUDIO
            },
            professional = MediQuo.CallViewModel.Professional(
                id = professionalHash,
                avatarURL = professionalAvatar?.takeIf { it.isNotBlank() }?.let(::URI),
                name = professionalName
            )
        )
    }

    fun showIncomingFromSocket(call: MediQuo.CallViewModel) {
        if (hasActiveIncomingCall()) {
            Log.d("SDKDemoCall", "Ignoring incoming socket call because another call is active")
            return
        }

        _incomingCallViewModel.value = call
    }

    fun clearIncomingCall() {
        _incomingCallViewModel.value = null
    }

    fun clearIncomingCallIfMatches(callId: String?) {
        val activeId = _incomingCallViewModel.value?.id ?: return
        if (!callId.isNullOrBlank() && callId == activeId) {
            _incomingCallViewModel.value = null
        }
    }
}

private enum class DemoDestination(val titleRes: Int) {
    ProfessionalList(R.string.show_professional_list),
    MedicalHistory(R.string.show_medical_history),
    Allergies(R.string.show_allergies),
    Diseases(R.string.show_diseases),
    MedicalReport(R.string.show_medical_reports),
    Medication(R.string.show_medication),
    Prescription(R.string.show_prescriptions),
    VideoCall(R.string.show_video_call),
    AudioCall(R.string.show_audio_call),
    AppointmentDetails(R.string.show_appointment_details),
    Chat(R.string.show_chat)
}

private fun DemoDestination.toViewKind(
    appointmentId: String,
    roomId: String,
    onSupportTapped: () -> Unit,
    onClose: () -> Unit
): MediQuo.ViewKind? {
    return when (this) {
        DemoDestination.ProfessionalList -> MediQuo.ViewKind.ProfessionalList(
            supportButton = MediQuo.SupportButtonConfiguration(
                title = "Support",
                onTap = onSupportTapped
            )
        )

        DemoDestination.MedicalHistory -> MediQuo.ViewKind.MedicalHistory
        DemoDestination.Allergies -> MediQuo.ViewKind.Allergies
        DemoDestination.Diseases -> MediQuo.ViewKind.Diseases
        DemoDestination.MedicalReport -> MediQuo.ViewKind.MedicalReport
        DemoDestination.Medication -> MediQuo.ViewKind.Medication
        DemoDestination.Prescription -> MediQuo.ViewKind.Prescription
        DemoDestination.VideoCall -> MediQuo.ViewKind.Call(
            callViewModel = MediQuo.CallViewModel.videoMock,
            closeHandler = onClose
        )

        DemoDestination.AudioCall -> MediQuo.ViewKind.Call(
            callViewModel = MediQuo.CallViewModel.audioMock,
            closeHandler = onClose
        )

        DemoDestination.AppointmentDetails -> {
            val id = appointmentId.trim()
            if (id.isEmpty()) null else MediQuo.ViewKind.AppointmentsDetails(id)
        }

        DemoDestination.Chat -> {
            val id = roomId.trim().toIntOrNull()
            if (id == null) null else MediQuo.ViewKind.Chat(id)
        }
    }
}
