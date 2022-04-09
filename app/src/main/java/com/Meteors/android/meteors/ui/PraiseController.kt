package com.Meteors.android.meteors.ui

import android.animation.Animator
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.util.Pools
import com.Meteors.android.meteors.R
import java.lang.IllegalStateException
import kotlin.random.Random

class PraiseController(private val context: Context, val viewGroup: ViewGroup) {

    private val mPraiseViewPool = PraiseViewPool(40)        //创建池模型保存PraiseView

    /**
     * @Description: 获取PraiseView，并对动画进行监听
     * @Param: PraiseView的坐标信息(x, y)
     */
    fun showPraise(x: Float, y: Float) {
        val praiseView = getPraiseView(x, y)
        val animatorSet = praiseView.getAnimatorSet()
        animatorSet.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {

            }

            /**
             * @Description: 动画结束后回收PraiseView， 并移除视图
             */
            override fun onAnimationEnd(animation: Animator?) {
                viewGroup.removeView(praiseView)
                mPraiseViewPool.release(praiseView)
            }

            override fun onAnimationCancel(animation: Animator?) {

            }

            override fun onAnimationRepeat(animation: Animator?) {

            }

        })
        animatorSet.start()     //开始动画
    }

    /**
     * @Description: 从常量池中获取PraiseView，并进行位置的初始化，这里可以进行PraiseView图像的定制， 可以进行图像资源的随机化
     * @Param:  View的坐标(x, y)
     * @return:  PraiseView
     */
    private fun getPraiseView(x: Float, y: Float): PraiseView {
        var praiseView = mPraiseViewPool.acquire()

        if (praiseView == null) {       //如果常量池返回为null，则创建新的实例
            praiseView = PraiseView(context)
            praiseView.layoutParams = ViewGroup.LayoutParams(160, 160)
            praiseView.scaleType = ImageView.ScaleType.FIT_XY
        }
        when (Random.nextInt(0, 10)) {
            in 0..2 -> praiseView.setImageResource(R.drawable.ic_action_praise_random1)
            in 3..5 -> praiseView.setImageResource(R.drawable.ic_action_praise_random2)
            else -> praiseView.setImageResource(R.drawable.ic_action_praise)
        }
        praiseView.x = x
        praiseView.y = y
        viewGroup.addView(praiseView)       //添加到布局中
        return praiseView
    }

    /**
     * @Description: PraiseView的常量池模型
     * @Param: 最大容量
     */
    inner class PraiseViewPool(maxPoolSize: Int) : Pools.Pool<PraiseView> {

        private var mPool: Array<PraiseView?>       //存储实例的数组

        private var mPoolSize = 0       //当前池内实例数量

        init {
            if (maxPoolSize <= 0) {
                throw IllegalArgumentException("The max pool size must be > 0")
            }
            mPool = Array(maxPoolSize, init = { null })         //初始化数组
        }

        /**
         * @Description: acquire接口获取实例
         */
        override fun acquire(): PraiseView? {
            if (mPoolSize > 0) {
                val lastIdx = mPoolSize - 1
                val instance: PraiseView = mPool[lastIdx] as PraiseView
                mPool[lastIdx] = null
                mPoolSize--
                return instance
            }
            return null
        }

        /**
         * @Description: release接口回收实例，放回数组中
         */
        override fun release(instance: PraiseView): Boolean {
            Log.d("test1", "mPoolSize = $mPoolSize")
            if (isInPool(instance)) {
                throw IllegalStateException("The PraiseView instance is already in the pool!")
            }
            if (mPoolSize < mPool.size) {
                mPool[mPoolSize] = instance
                mPoolSize++
                return true
            }
            return false
        }

        /**
         * @Description: 判断实例是否已经存在
         */
        private fun isInPool(instance: PraiseView): Boolean {
            for (i in 0 until mPoolSize) {
                if (mPool[i] == instance) {
                    return true
                }
            }
            return false
        }

    }
}