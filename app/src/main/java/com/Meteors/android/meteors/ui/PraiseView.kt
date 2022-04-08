package com.Meteors.android.meteors.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.TypeEvaluator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import kotlin.random.Random

private const val TAG = "Meteors_PraiseView"

/**
 * @Description: PraiseView为屏幕点心动画，继承自AppCompatImageView
 */
class PraiseView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    /**
     * @Description: 点击事件也伴随动画
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        getAnimatorSet().start()
        return super.onTouchEvent(event)
    }

    /**
     * @Description: 自定义路径属性Location， 搭配Point使用完成贝塞尔路径的计算
     * @Param: Point根据评估程序计算出的当前位置
     */
    fun setLocation(point: Point) {
        this.translationX = point.x.toFloat()
        this.translationY = point.y.toFloat()
    }

    /**
     * @Description: 进行动画的制作
     * @return: AnimatorSet控制动画
     */
    fun getAnimatorSet(): AnimatorSet {
        val animatorSet = AnimatorSet()
        val x = this.x.toInt()      //动画起点坐标
        val y = this.y.toInt()
        val offset = Random.nextInt(-200, 200)      //对贝塞尔曲线的控制点取随机偏移
        val eva = BesselTypeEvaluator(
            Point(x - 100 - offset, y - 600 + offset),
            Point(x + 100 + offset, y - 600 - offset)
        )
        val moveAnimator =
            ObjectAnimator.ofObject(this, "location", eva, Point(x, y), Point(x, y - 1800))
        moveAnimator.duration = 1500
        val alphaAnimator = ObjectAnimator.ofFloat(this, "alpha", 0.3f, 1f, 0.8f, 0f)
        alphaAnimator.duration = 1500
        val xAnimator = ObjectAnimator.ofFloat(this, "scaleX", 0f, 1f)
        val yAnimator = ObjectAnimator.ofFloat(this, "scaleY", 0f, 1f)
        xAnimator.duration = 300
        yAnimator.duration = 300

        animatorSet.play(moveAnimator).with(alphaAnimator).with(xAnimator).with(yAnimator)
        return animatorSet
    }

    /**
     * @Description: 自定义Point类型评估程序， 计算三姐贝塞尔曲线路径
     * @Param: point1， point2为两个控制点
     * @return: TypeEvaluator
     */
    class BesselTypeEvaluator(
        private val controllerPoint1: Point,
        private val controllerPoint2: Point
    ) : TypeEvaluator<Point> {

        //B (t) = P0 * (1-t)^3 + 3 * P1 * t * (1-t)^2 + 3 * P2 * t^2 * (1-t) + P3 * t^3
        override fun evaluate(fraction: Float, startValue: Point?, endValue: Point?): Point {
            val x = startValue!!.x * (1 - fraction) * (1 - fraction) * (1 - fraction) +
                    3 * controllerPoint1.x * fraction * (1 - fraction) * (1 - fraction) +
                    3 * controllerPoint2.x * fraction * fraction * (1 - fraction) +
                    endValue!!.x * fraction * fraction * fraction
            val y = startValue.y * (1 - fraction) * (1 - fraction) * (1 - fraction) +
                    3 * controllerPoint1.y * fraction * (1 - fraction) * (1 - fraction) +
                    3 * controllerPoint2.y * fraction * fraction * (1 - fraction) +
                    endValue.y * fraction * fraction * fraction
            return Point(x.toInt(), y.toInt())
        }
    }
}

