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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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

class DemoHostActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val destination = intent.getStringExtra(EXTRA_DESTINATION)
            ?.let(DemoDestination::valueOf)
        val appointmentId = intent.getStringExtra(EXTRA_APPOINTMENT_ID).orEmpty()
        val roomId = intent.getStringExtra(EXTRA_ROOM_ID).orEmpty()
        val sdk = (applicationContext as? App)?.currentSdk()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewKind = remember(destination, appointmentId, roomId) {
                        destination?.toViewKind(
                            appointmentId = appointmentId,
                            roomId = roomId,
                            onSupportTapped = {
                                Toast.makeText(
                                    this@DemoHostActivity,
                                    getString(R.string.support_tapped),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onClose = { finish() }
                        )
                    }

                    if (sdk == null || destination == null || viewKind == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(WindowInsets.safeDrawing)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when {
                                    sdk == null -> getString(R.string.sdk_not_ready)
                                    destination == DemoDestination.AppointmentDetails -> getString(R.string.invalid_appointment_id)
                                    destination == DemoDestination.Chat -> getString(R.string.invalid_room_id)
                                    else -> getString(R.string.unavailable_demo)
                                }
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(WindowInsets.safeDrawing)
                        ) {
                            sdk.sdkView(
                                kind = viewKind,
                                modifier = Modifier.fillMaxSize(),
                                onClose = { finish() }
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_DESTINATION = "extra_destination"
        private const val EXTRA_APPOINTMENT_ID = "extra_appointment_id"
        private const val EXTRA_ROOM_ID = "extra_room_id"

        fun createIntent(
            context: android.content.Context,
            destination: DemoDestination,
            appointmentId: String,
            roomId: String
        ): Intent {
            return Intent(context, DemoHostActivity::class.java)
                .putExtra(EXTRA_DESTINATION, destination.name)
                .putExtra(EXTRA_APPOINTMENT_ID, appointmentId)
                .putExtra(EXTRA_ROOM_ID, roomId)
        }
    }
}

@Composable
private fun SDKDemoApp(
    onAskNotificationPermissions: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as App
    var appointmentId by rememberSaveable { mutableStateOf("") }
    var roomId by rememberSaveable { mutableStateOf("") }
    var uiErrorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val sdkInitializationState by app.sdkInitializationState().collectAsState()
    val incomingCallViewModel by DemoIncomingCallStore.incomingCallViewModel.collectAsState()
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

    val activeSdk = sdkInitializationState.sdk
    val validationErrorMessage = uiErrorMessage
    val sdkErrorMessage = sdkInitializationState.errorMessage
    val isLoading = sdkInitializationState.isLoading

    LaunchedEffect(activeSdk, eventDelegate) {
        activeSdk?.let {
            it.eventDelegate = eventDelegate
            uiErrorMessage = null
            onAskNotificationPermissions()
        }
    }

    fun openDemo(destination: DemoDestination) {
        if (activeSdk == null) return

        val isInputValid = when (destination) {
            DemoDestination.AppointmentDetails -> appointmentId.trim().isNotEmpty()
            DemoDestination.Chat -> roomId.trim().toIntOrNull() != null
            else -> true
        }

        if (!isInputValid) {
            uiErrorMessage = when (destination) {
                DemoDestination.AppointmentDetails -> context.getString(R.string.invalid_appointment_id)
                DemoDestination.Chat -> context.getString(R.string.invalid_room_id)
                else -> context.getString(R.string.unavailable_demo)
            }
            scope.launch {
                scrollState.animateScrollTo(0)
            }
            return
        }

        uiErrorMessage = null
        context.startActivity(
            DemoHostActivity.createIntent(
                context = context,
                destination = destination,
                appointmentId = appointmentId,
                roomId = roomId
            )
        )
    }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (activeSdk != null) {
                        Modifier.verticalScroll(scrollState)
                    } else {
                        Modifier
                    }
                ),
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

            validationErrorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )
            }

            sdkErrorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    onClick = {
                        uiErrorMessage = null
                        app.initializeSdkIfNeeded(forceRetry = true)
                    }
                ) {
                    Text(stringResource(R.string.retry_sdk_initialization))
                }
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
                        onClick = { openDemo(destination) }
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

        if (activeSdk == null && isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
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

enum class DemoDestination(val titleRes: Int) {
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
