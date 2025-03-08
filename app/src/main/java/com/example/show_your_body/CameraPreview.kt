package com.example.show_your_body

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.show_your_body.databinding.CameraPreviewBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraPreview @JvmOverloads constructor(
    private val lifecycleOwner: LifecycleOwner,
    context: Context,
    attrs: AttributeSet? = null,
    private val getBackendUrl: () -> String? = { null }
) : FrameLayout(context, attrs) {

    // ViewBinding for camera_preview.xml
    private val binding: CameraPreviewBinding =
        CameraPreviewBinding.inflate(LayoutInflater.from(context), this, true)

    // Pose detector shared across frames
    private val poseDetector = PoseDetector(context)

    // Executor for background frame processing
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // 1) Preview Use Case (just to see the camera)
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            // 2) ImageAnalysis Use Case (for real-time frames)
            val imageAnalysis = ImageAnalysis.Builder()
                // Keep only the latest frame
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            // Attach our custom Analyzer
            imageAnalysis.setAnalyzer(
                cameraExecutor,
                PoseAnalyzer(
                    poseDetector,
                    { keypoints ->
                        binding.overlayView.keypoints = keypoints
                        binding.overlayView.postInvalidate()
                    },
                    context,
                    getBackendUrl
                )
            )

            try {
                // Unbind any existing use cases, then bind ours
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e("CameraPreview", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }
}