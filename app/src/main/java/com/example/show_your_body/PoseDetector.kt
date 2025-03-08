package com.example.show_your_body

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class PoseDetector(context: Context) {
    private var interpreter: Interpreter? = null

    // In PoseDetector.kt
    init {
        val options = Interpreter.Options().apply {
            setUseNNAPI(true)
            setNumThreads(4)
            // Add more memory for accuracy
            setAllowFp16PrecisionForFp32(true)
        }

        interpreter = Interpreter(loadModelFile(context), options)
    }

    private fun loadModelFile(context: Context): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd("movenet_thunder.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    /**
     * The MoveNet Thunder (float16) model expects:
     *   - Input: [1, 256, 256, 3] in uint8 (values 0..255).
     *   - Output: [1, 1, 17, 3].
     *
     * We'll convert the output into [17][3] => an Array of FloatArray,
     * where each row is [y, x, confidence].
     */
    fun detectPose(bitmap: Bitmap): Array<FloatArray> {
        // 1) Prepare ByteBuffer for [1,256,256,3] with raw bytes
        val inputBuffer = ByteBuffer.allocateDirect(256 * 256 * 3) // 1 batch
        inputBuffer.order(ByteOrder.nativeOrder())

        // Extract ARGB from the bitmap
        val intValues = IntArray(256 * 256)
        bitmap.getPixels(intValues, 0, 256, 0, 0, 256, 256)

        // Fill ByteBuffer with R, G, B in [0..255]
        for (pixel in intValues) {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            inputBuffer.put(r.toByte())
            inputBuffer.put(g.toByte())
            inputBuffer.put(b.toByte())
        }

        // 2) The raw output shape is [1,1,17,3]
        val rawOutput = Array(1) { Array(1) { Array(17) { FloatArray(3) } } }

        // 3) Run inference
        interpreter?.run(inputBuffer, rawOutput)

        // 4) Flatten the output into [17][3]
        val keypoints = Array(17) { FloatArray(3) }
        for (i in 0 until 17) {
            // rawOutput[0][0][i] = float[3] => [y, x, confidence]
            keypoints[i][0] = rawOutput[0][0][i][0] // y
            keypoints[i][1] = rawOutput[0][0][i][1] // x
            keypoints[i][2] = rawOutput[0][0][i][2] // confidence
        }

        return keypoints
    }
}
