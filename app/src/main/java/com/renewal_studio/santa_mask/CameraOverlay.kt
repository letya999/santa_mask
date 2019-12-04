package com.renewal_studio.santa_mask

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.Surface
import android.view.View
import android.view.WindowManager
import com.google.android.cameraview.CameraView
import com.tzutalin.dlib.VisionDetRet
import org.jetbrains.annotations.NotNull

class CameraOverlay(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val rect = RectF()
    private val r = Rect()
    var face: Rect? = null
    lateinit var preview: CameraView
    @NotNull
    private var results: List<VisionDetRet>? = null

    fun setFaceResults(results: List<VisionDetRet>?) {
        this.results = results
    }

    fun getDisplayRotation() : Int {
        val rotation = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                .defaultDisplay
                .rotation
        val displayRotation = when (rotation) {
            Surface.ROTATION_0 -> 90    // portrait
            Surface.ROTATION_90 -> 0    // landscape-left
            Surface.ROTATION_270 -> 180 // landscape-right
            else -> 90
        }
        return displayRotation
    }

    private fun adjustPoint(x0: Long, y0: Long) : Pair<Float, Float> {
        r.set(face)
        r.mapTo(preview.width, preview.height, getDisplayRotation())
        val x1 = x0.toFloat() / r.left
        val y1 = y0.toFloat() / r.top
        val x = x1 * rect.left
        val y = y1 * rect.top
        return Pair(x, y)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if(results!=null) {
            for (ret in this.results!!) {
                val resizeRatio = 4.5f
                val landmarks = ret.getFaceLandmarks()
                for (point in landmarks) {
                    val pointX = (point.x * resizeRatio).toInt()
                    val pointY = (point.y * resizeRatio).toInt()
                    canvas.drawCircle(pointX.toFloat(), pointY.toFloat(), 4f, pPaint)
                }
            }
        }
        face = null
    }

    companion object {
        private val rPaint = Paint()
        private val pPaint = Paint()
        private val white = Paint()

        init {
            rPaint.color = Color.rgb(255, 160, 0)
            rPaint.style = Paint.Style.STROKE
            rPaint.strokeWidth = 5f
            pPaint.color = Color.YELLOW
            pPaint.style = Paint.Style.FILL
            white.color = Color.WHITE
            white.style = Paint.Style.STROKE
            white.textSize = 30f
        }
    }
}