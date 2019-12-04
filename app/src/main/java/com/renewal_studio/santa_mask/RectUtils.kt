package com.renewal_studio.santa_mask

import android.graphics.Rect

const val portrait  = 90
const val landleft  = 0
const val landright = 180

fun Rect.inBounds(left: Int = 0, top: Int = 0, right: Int, bottom: Int): Rect {
    val w = this.width()
    val h = this.height()

    if (this.left < left)
        this.left = left

    if (this.right > right)
        this.right = right

    if (this.top < top)
        this.top = top

    if (this.bottom > bottom)
        this.bottom = bottom

    return this
}

fun Rect.repr() = "[Rect] (left: $left top: $top), (right: $right bottom: $bottom)"

fun Rect.scale(xScale: Float = 1f, yScale: Float = 1f): Rect {
    val dw = (((this.width()  * xScale) - this.width())  * .5).toInt()
    val dh = (((this.height() * yScale) - this.height()) * .5).toInt()

    this.left   = this.left   - dw
    this.right  = this.right  + dw
    this.top    = this.top    - dh
    this.bottom = this.bottom + dh

    return this
}

fun Rect.mapTo(width: Int, height: Int, rotation: Int): Rect {
    val hw = this.width() / 2f
    val hh = this.height() / 2f
    val cx = this.left + hw
    val cy = this.top  + hh
    val side = (hh + hw) / 2f

    val left   = cx - side
    val right  = cx + side
    val top    = cy - side
    val bottom = cy + side

    val l = (left   + 1000) / 2000f
    val r = (right  + 1000) / 2000f
    val t = (top    + 1000) / 2000f
    val b = (bottom + 1000) / 2000f

    when (rotation) {
        portrait -> {
            val w = if (width > height) height else width
            val h = if (width > height) width  else height

            this.left   = Math.round(w - (w * b))
            this.right  = Math.round(w - (w * t))
            this.top    = Math.round(h - (h * r))
            this.bottom = Math.round(h - (h * l))
        }

        landleft -> {
            val w = if (width < height) height else width
            val h = if (width < height) width  else height

            this.left   = Math.round(w - (w * r))
            this.right  = Math.round(w - (w * l))
            this.top    = Math.round(h * t)
            this.bottom = Math.round(h * b)

            val wr = this.width()  * .5f
            val hr = this.height() * .5f
            val x0 = this.centerX()
            val y0 = this.centerY()

            this.left   = (x0 - hr).toInt()
            this.right  = (x0 + hr).toInt()
            this.top    = (y0 - wr).toInt()
            this.bottom = (y0 + wr).toInt()
        }

        landright -> {
            val w = if (width < height) height else width
            val h = if (width < height) width  else height

            this.left   = Math.round(w * l)
            this.right  = Math.round(w * r)
            this.top    = Math.round(h - (h * b))
            this.bottom = Math.round(h - (h * t))

            val wr = this.width()  * .5f
            val hr = this.height() * .5f
            val x0 = this.centerX()
            val y0 = this.centerY()

            this.left   = (x0 - hr).toInt()
            this.right  = (x0 + hr).toInt()
            this.top    = (y0 - wr).toInt()
            this.bottom = (y0 + wr).toInt()
        }
    }

    return this
}