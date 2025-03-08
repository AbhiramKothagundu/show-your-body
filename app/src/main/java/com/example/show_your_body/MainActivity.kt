package com.example.show_your_body

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.show_your_body.ui.theme.ShowyourbodyTheme

class MainActivity : ComponentActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var backendUrlInput: EditText

    private val URL_PREF_KEY = "backend_url"

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted
            setUIContent()
        } else {
            // Permission denied
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize shared preferences
        sharedPreferences = getPreferences(Context.MODE_PRIVATE)

        // Setup UI components
        backendUrlInput = findViewById(R.id.backend_url)
        val saveButton = findViewById<Button>(R.id.save_url_button)

        // Load saved URL
        val savedUrl = sharedPreferences.getString(URL_PREF_KEY, "")
        backendUrlInput.setText(savedUrl)

        // Setup save button
        saveButton.setOnClickListener {
            val url = backendUrlInput.text.toString().trim()
            saveBackendUrl(url)
            Toast.makeText(this, "URL saved", Toast.LENGTH_SHORT).show()
        }

        // Request camera permission if needed
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        } else {
            setupCamera()
        }
    }

    private fun saveBackendUrl(url: String) {
        sharedPreferences.edit().putString(URL_PREF_KEY, url).apply()
    }

    private fun getBackendUrl(): String? {
        val url = sharedPreferences.getString(URL_PREF_KEY, "")
        return if (url.isNullOrBlank()) null else url
    }

    private fun setUIContent() {
        setContent {
            ShowyourbodyTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { context ->
                            // Pass THIS activity as the lifecycleOwner
                            CameraPreview(
                                lifecycleOwner = this@MainActivity,
                                context = context
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    private fun setupCamera() {
        // Create and add CameraPreview to the container
        val cameraContainer = findViewById<android.widget.FrameLayout>(R.id.camera_container)

        val cameraPreview = CameraPreview(
            lifecycleOwner = this,
            context = this,
            getBackendUrl = { getBackendUrl() }
        )

        cameraContainer.addView(cameraPreview)
    }



}
