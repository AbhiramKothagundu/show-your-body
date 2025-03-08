package com.example.show_your_body

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class OverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    // We expect a single row with 51 values: 17 keypoints * 3 (x, y, confidence)
    var keypoints: Array<FloatArray>? = null

    // Confidence threshold for drawing keypoints
    private val CONFIDENCE_THRESHOLD = 0.3f

    private val pointPaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 5f
        style = Paint.Style.FILL
    }

    private val linePaint = Paint().apply {
        color = Color.YELLOW
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    // Define the connections between keypoints for the skeleton
    // MoveNet Lightning/Thunder keypoints (17 keypoints total):
    // 0: nose, 1: left_eye, 2: right_eye, 3: left_ear, 4: right_ear, 5: left_shoulder,
    // 6: right_shoulder, 7: left_elbow, 8: right_elbow, 9: left_wrist, 10: right_wrist,
    // 11: left_hip, 12: right_hip, 13: left_knee, 14: right_knee, 15: left_ankle, 16: right_ankle
    private val skeletonConnections = listOf(
        Pair(0, 1), Pair(0, 2), // Nose to eyes
        Pair(1, 3), Pair(2, 4), // Eyes to ears
        Pair(5, 6), // Left to right shoulder
        Pair(5, 7), Pair(7, 9), // Left arm
        Pair(6, 8), Pair(8, 10), // Right arm
        Pair(5, 11), Pair(6, 12), // Shoulders to hips
        Pair(11, 12), // Left to right hip
        Pair(11, 13), Pair(13, 15), // Left leg
        Pair(12, 14), Pair(14, 16) // Right leg
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        keypoints?.let { kps ->
            if (kps.isNotEmpty()) {
                val points = Array(17) { Point() }

                // Each keypoint is already a FloatArray [y, x, confidence]
                for (i in 0 until 17) {
                    val keypoint = kps[i]  // This is already [y, x, confidence]
                    val yNorm = keypoint[0]  // Y coordinate
                    val xNorm = keypoint[1]  // X coordinate
                    val confidence = keypoint[2]  // Confidence

                    points[i].x = (1 - yNorm) * width
                    points[i].y = xNorm * height
                    points[i].confidence = confidence
                }

                // Draw the skeleton lines with thicker lines for better visibility
                linePaint.strokeWidth = 5f
                for (connection in skeletonConnections) {
                    val from = points[connection.first]
                    val to = points[connection.second]

                    // Only draw if both points have sufficient confidence
                    if (from.confidence > CONFIDENCE_THRESHOLD && to.confidence > CONFIDENCE_THRESHOLD) {
                        canvas.drawLine(from.x, from.y, to.x, to.y, linePaint)
                    }
                }

                // Draw the keypoints with larger circles
                for (point in points) {
                    if (point.confidence > CONFIDENCE_THRESHOLD) {
                        canvas.drawCircle(point.x, point.y, 10f, pointPaint)
                    }
                }
            }
        }
    }

    private class Point {
        var x: Float = 0f
        var y: Float = 0f
        var confidence: Float = 0f
    }
}