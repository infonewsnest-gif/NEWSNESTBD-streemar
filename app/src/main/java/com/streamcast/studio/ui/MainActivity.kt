package com.streamcast.studio.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.streamcast.studio.scene.SceneLayout
import com.streamcast.studio.service.StreamBroadcastService
import com.streamcast.studio.ui.theme.StreamCastTheme

class MainActivity : ComponentActivity() {

    private lateinit var callbackManager: CallbackManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.all { it.value }) {
            Toast.makeText(this, "স্টোরেজ এবং অডিও পারমিশন প্রয়োজন!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()

        callbackManager = CallbackManager.Factory.create()

        setContent {
            StreamCastTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isLive by remember { mutableStateOf(false) }
                    var isFacebookLoggedIn by remember { mutableStateOf(false) }
                    
                    var streamPlatform by remember { mutableStateOf("Facebook") }
                    var caption by remember { mutableStateOf("") }
                    var streamKey by remember { mutableStateOf("") }
                    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
                    var breakingNewsText by remember { mutableStateOf("ব্রেকিং নিউজ: নিউজ নেস্ট স্ট্রিমিং অ্যাপ থেকে লাইভ সম্প্রচার চলছে...") }

                    val videoPickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri: Uri? ->
                        uri?.let {
                            selectedVideoUri = it
                            Toast.makeText(this@MainActivity, "ভিডিও সিলেক্ট করা হয়েছে", Toast.LENGTH_SHORT).show()
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        SceneLayout(
                            videoUri = selectedVideoUri,
                            tickerText = breakingNewsText,
                            modifier = Modifier.fillMaxSize()
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { streamPlatform = "Facebook" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (streamPlatform == "Facebook") Color(0xFF1877F2) else Color.DarkGray
                                    ),
                                    modifier = Modifier.weight(1.5f).height(40.dp)
                                ) {
                                    Text("ফেসবুক মোড", color = Color.White)
                                }

                                Button(
                                    onClick = { streamPlatform = "YouTube" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (streamPlatform == "YouTube") Color(0xFFFF0000) else Color.DarkGray
                                    ),
                                    modifier = Modifier.weight(1.5f).height(40.dp)
                                ) {
                                    Text("ইউটিউব মোড", color = Color.White)
                                }
                            }

                            if (streamPlatform == "Facebook") {
                                Button(
                                    onClick = {
                                        if (!isFacebookLoggedIn) {
                                            LoginManager.getInstance().logInWithPublishPermissions(
                                                this@MainActivity,
                                                listOf("pages_show_list", "pages_manage_posts", "live_video")
                                            )
                                            LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                                                override fun onSuccess(result: LoginResult) {
                                                    isFacebookLoggedIn = true
                                                    Toast.makeText(this@MainActivity, "ফেসবুক এসডিকে লগইন সফল!", Toast.LENGTH_SHORT).show()
                                                }
                                                override fun onCancel() {}
                                                override fun onError(error: FacebookException) {
                                                    Toast.makeText(this@MainActivity, "ত্রুটি: ${error.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            })
                                        } else {
                                            LoginManager.getInstance().logOut()
                                            isFacebookLoggedIn = false
                                            Toast.makeText(this@MainActivity, "ফেসবুক লগআউট হয়েছে", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isFacebookLoggedIn) Color.Gray else Color(0xFF1877F2)),
                                    modifier = Modifier.fillMaxWidth().height(40.dp)
                                ) {
                                    Text(text = if (isFacebookLoggedIn) "ফেসবুক লগআউট (SDK)" else "ফেসবুক লগইন (SDK)", color = Color.White)
                                }
                            }

                            OutlinedTextField(
                                value = streamKey,
                                onValueChange = { streamKey = it },
                                label = { Text(if (streamPlatform == "Facebook") "ফেসবুক Stream Key / API Key" else "ইউটিউব Stream Key / API Key") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Black.copy(alpha = 0.6f),
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.4f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = { videoPickerLauncher.launch("video/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                            ) {
                                Text(text = "ফাইল ম্যানেজার থেকে ভিডিও সিলেক্ট করুন", color = Color.White)
                            }

                            OutlinedTextField(
                                value = caption,
                                onValueChange = { caption = it },
                                label = { Text("লাইভ ক্যাপশন / টাইটেল লিখুন...") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Black.copy(alpha = 0.6f),
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.4f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    if (!isLive) {
                                        if (selectedVideoUri == null) {
                                            Toast.makeText(this@MainActivity, "দয়া করে একটি ভিডিও সিলেক্ট করুন", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (streamKey.isBlank() && !isFacebookLoggedIn) {
                                            Toast.makeText(this@MainActivity, "দয়া করে Stream Key দিন অথবা SDK দিয়ে লগইন করুন", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        isLive = true
                                        startLiveStream(streamPlatform, caption, streamKey, selectedVideoUri.toString())
                                    } else {
                                        isLive = false
                                        stopLiveStream()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isLive) Color.Red else Color(0xFF00C853)
                                ),
                                modifier = Modifier.fillMaxWidth().height(45.dp)
                            ) {
                                Text(
                                    text = if (isLive) "লাইভ বন্ধ করুন" else "$streamPlatform লাইভ শুরু করুন",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (!permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun startLiveStream(platform: String, caption: String, streamKey: String, videoUri: String) {
        val intent = Intent(this, StreamBroadcastService::class.java).apply {
            action = "ACTION_START_STREAM"
            putExtra("EXTRA_PLATFORM", platform)
            putExtra("EXTRA_CAPTION", caption)
            putExtra("EXTRA_STREAM_KEY", streamKey)
            putExtra("EXTRA_VIDEO_URI", videoUri)
        }
        startForegroundService(intent)
        Toast.makeText(this, "$platform লাইভ শুরু হচ্ছে...", Toast.LENGTH_SHORT).show()
    }

    private fun stopLiveStream() {
        val intent = Intent(this, StreamBroadcastService::class.java).apply {
            action = "ACTION_STOP_STREAM"
        }
        startService(intent)
        Toast.makeText(this, "লাইভ বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show()
    }
}
