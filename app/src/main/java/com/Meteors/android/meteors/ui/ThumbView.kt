package com.Meteors.android.meteors.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.Animation
import android.view.animation.AnticipateInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import com.Meteors.android.meteors.R

private const val TAG = "Meteors_ThumbView"

/**
 * ThumbView点赞动画
 */
class ThumbView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {
    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    private var isChecked = false

    private var canvas: Canvas? = null

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        this.canvas = canvas
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    @SuppressLint("UseCompatLoadingForDrawables", "Recycle")
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        background = if(!isChecked){
            resources.getDrawable(R.drawable.ic_action_thumb_up, context.theme)
        }else{
            resources.getDrawable(R.drawable.ic_action_thumb, context.theme)
        }

        /*
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_action_thumb_small)
        val paint = Paint()
        paint.color = Color.RED
        paint.colorFilter = object: PorterDuffColorFilter(Color.RED, PorterDuff.Mode.SRC_IN){}
        canvas?.drawBitmap(bitmap, 0f, 0f, paint)
         */

        //放大动画
        val animatorSet = AnimatorSet()
        val xAnimator = ObjectAnimator.ofFloat(this, "scaleX", 0.7f, 1.5f ,1f)
        val yAnimator = ObjectAnimator.ofFloat(this, "scaleY", 0.7f, 1.5f, 1f)
        xAnimator.interpolator = object: AnticipateOvershootInterpolator(){}
        yAnimator.interpolator = object: AnticipateOvershootInterpolator(){}
        xAnimator.duration = 1000
        yAnimator.duration = 1000
        animatorSet.play(xAnimator).with(yAnimator)
        animatorSet.start()

        return true
    }

}