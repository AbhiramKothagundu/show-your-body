package com.example.show_your_body

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class PoseAnalyzer(
    private val poseDetector: PoseDetector,
    private val onPoseDetected: (Array<FloatArray>) -> Unit, // Expects [17][3]
    private val context: Context,
    private val backendUrlProvider: () -> String?
) : ImageAnalysis.Analyzer {

    private val networkScope = CoroutineScope(Dispatchers.IO)

    // Frame counter to limit how often we send data to backend
    private var frameCounter = 0
    private val SEND_EVERY_N_FRAMES = 10

    private var uiFrameCounter = 0
    private val UPDATE_UI_EVERY_N_FRAMES = 2

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        uiFrameCounter++
        if (uiFrameCounter % UPDATE_UI_EVERY_N_FRAMES != 0) {
            imageProxy.close()
            return
        }

        try {
            // Convert to ARGB_8888 Bitmap
            val bitmap = imageProxy.toBitmap(context) ?: return

            // The model expects 256×256
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, true)


            // Detect pose => returns [17][3]
            val keypoints = poseDetector.detectPose(resizedBitmap)

            // Show skeleton on screen
            onPoseDetected(keypoints)

            // Optionally send keypoints to your backend
            frameCounter++
            if (frameCounter % SEND_EVERY_N_FRAMES == 0) {
                sendKeypointsToBackend(keypoints)
            }

        } finally {
            imageProxy.close()
        }
    }

    private fun sendKeypointsToBackend(keypoints: Array<FloatArray>) {
        val backendUrl = backendUrlProvider() ?: return

        networkScope.launch {
            try {
                // Format the keypoints as JSON
                val jsonKeypoints = JSONArray()

                // keypoints is [17][3] => each row is [y, x, confidence]
                for (i in keypoints.indices) {
                    val y = keypoints[i][0]
                    val x = keypoints[i][1]
                    val confidence = keypoints[i][2]

                    val keypointObj = JSONObject()
                    keypointObj.put("x", x)
                    keypointObj.put("y", y)
                    keypointObj.put("confidence", confidence)
                    jsonKeypoints.put(keypointObj)
                }

                val jsonBody = JSONObject()
                jsonBody.put("keypoints", jsonKeypoints)
                jsonBody.put("timestamp", System.currentTimeMillis())

                // POST to your server
                val url = URL(backendUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(jsonBody.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                // Optionally handle non-2xx responses
                connection.disconnect()

            } catch (e: Exception) {
                // Log error or handle it
            }
        }
    }
}
