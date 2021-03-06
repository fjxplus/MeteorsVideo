package com.Meteors.android.meteors.ui.LiveVideo

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnticipateInterpolator
import android.view.animation.OvershootInterpolator
import com.Meteors.android.meteors.R
import com.Meteors.android.meteors.databinding.GiftAnimationLayoutBinding
import kotlin.math.round

/**
 * @Description: 管理礼物赠送动画的执行，对礼物单击和连击进行处理
 * @Param: startY礼物展示区域的顶坐标，endY底部坐标，size为礼物动画的最大并发显示个数
 */
class GiftManager(
    private val context: Context,
    private val container: ViewGroup,
    private val size: Int,
    private val winWidth: Int,
    startY: Int = 0,
    endY: Int
) {

    lateinit var setCountDownTick: (millisUntilFinished: Int) -> Unit       //回调返回倒计时

    lateinit var endSendCallback: () -> Unit        //连击结束回调

    //每个礼物动画View的高度， 用于初始化View
    private val giftHeight = (endY - startY) / size

    //View数组，存储礼物动画View，进行初始化（宽高，坐标设置在屏幕外侧），并添加到根布局中
    private val giftContainer = Array(size, init = { i ->
        GiftAnimationLayoutBinding.inflate(LayoutInflater.from(context)).apply {
            root.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                giftHeight
            )
            root.x = -winWidth.toFloat()       //横坐标在屏幕外侧
            root.y =
                (endY - (i + 1) * giftHeight).toFloat()    //纵坐标根据startY（顶部）和endY（底部）计算，自底向上
            container.addView(root)
        }
    })

    //标志数组，记录giftContainer数组中View的使用情况
    private val sign = IntArray(size, init = { 1 })

    //公屏展示的拉出动画，坐标x移动到当前屏幕左侧
    private val startAnimator = Array<ObjectAnimator>(size, init = { i ->
        val animatorStart =
            ObjectAnimator.ofFloat(giftContainer[i].root, "x", 0f)
        animatorStart.interpolator = object : OvershootInterpolator() {}
        animatorStart
    })

    //连击时的字体放大动画
    private val textScaleAnimator = Array<AnimatorSet>(size, init = {i ->
        val animatorSet = AnimatorSet()
        val textAnimatorX = ObjectAnimator.ofFloat(giftContainer[i].textGiftCount, "scaleX", 1f, 0.2f, 1.5f, 1f)
        val textAnimatorY = ObjectAnimator.ofFloat(giftContainer[i].textGiftCount, "scaleY", 1f, 0.2f, 1.5f, 1f)
        textAnimatorX.duration = 300
        textAnimatorY.duration = 300
        animatorSet.play(textAnimatorX).with(textAnimatorY)
        animatorSet
    })

    //公屏展示的结束动画，x坐标设为屏幕左侧
    private val endAnimator = Array<ObjectAnimator>(size, init = { i ->
        val animatorEnd =
            ObjectAnimator.ofFloat(giftContainer[i].root, "x", -winWidth.toFloat())
        animatorEnd.interpolator = object : AnticipateInterpolator() {}
        animatorEnd
    })

    //礼物单击的展示动画，一段时间间隔后自动执行结束动画，并重置标志位
    private val onceAnimatorSet = Array(size, init = { i ->
        val animatorSet = AnimatorSet()
        animatorSet.play(endAnimator[i]).after(700).after(startAnimator[i])

        animatorSet.addListener(object : Animator.AnimatorListener {

            override fun onAnimationStart(animation: Animator?) {}
            override fun onAnimationCancel(animation: Animator?) {}
            override fun onAnimationRepeat(animation: Animator?) {}

            override fun onAnimationEnd(animation: Animator?) {
                sign[i] = 1     //动画结束后标志位重置1
            }
        })
        animatorSet
    })

    //记录连击次数
    private var doubleTimes = 0

    //记录连击动画占用的View索引
    private var doubleIndex: Int? = null

    //连击定时器，定时结束后初始化doubleTimes，doubleIndex，并播放结束动画
    private val countDownTimer = object : CountDownTimer(3000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            //回调，更新连击按钮的数字
            if (!this@GiftManager::setCountDownTick.isInitialized || !this@GiftManager::endSendCallback.isInitialized) {
                throw IllegalStateException("未在Fragment/Activity中实例化回调属性setCountDownTick: () -> Unit 或 endSendCallback: () -> Unit")
            }
            setCountDownTick(round(millisUntilFinished / 1000.0).toInt())
        }

        override fun onFinish() {
            if (doubleIndex != null) {
                endAnimator[doubleIndex!!].start()      //播放结束动画， 收起View
                endSendCallback()
                sign[doubleIndex!!] = 1         //标志位重置为1
                doubleIndex = null      //当前连击动画的索引置null
                doubleTimes = 0         //连击次数置0
            }
        }

    }

    /**
     * @Description: 开启礼物单击动画
     */
    fun showGiftOnce(userName: String, giftType: Int) {
        val viewIndex = getGiftShowView()
        if (viewIndex != null) {
            //设置数据
            val itemBinding = giftContainer[viewIndex]
            itemBinding.textUserId.text = userName
            itemBinding.imageGift.setImageResource(giftId[giftType])
            itemBinding.textGiftCount.text = ""
            onceAnimatorSet[viewIndex].start()
        }
    }

    /**
     * @Description: 开启礼物动画，记录点击次数，取消计时器重新计时
     */
    @SuppressLint("SetTextI18n")
    fun showGiftDouble(userName: String, giftType: Int) {
        doubleTimes++
        if (doubleIndex == null) {        //还没申请到展示动画的View
            doubleIndex = getGiftShowView()     //申请
            if (doubleIndex != null) {
                //如果申请到View，开启展示动画
                startAnimator[doubleIndex!!].start()
            }
        }

        if (doubleIndex != null) {       //当前礼物正在显示
            //取消定时
            countDownTimer.cancel()
            //更新数据
            val itemBinding = giftContainer[doubleIndex!!]
            itemBinding.textUserId.text = userName
            itemBinding.imageGift.setImageResource(giftId[giftType])
            textScaleAnimator[doubleIndex!!].start()
            itemBinding.textGiftCount.text = "X$doubleTimes"
            //重新开始定时
            countDownTimer.start()
        }
    }

    /**
     * @Description: 结束礼物连击霸屏
     */
    fun endShowGiftDouble() {
        countDownTimer.cancel()
        countDownTimer.onFinish()
    }

    /**
     * @Description: 找到空闲的礼物公屏View，靠近下面（下标小）的优先
     * @return:  空闲View在数组中的下标， 没有空闲的化返回null
     */
    private fun getGiftShowView(): Int? {
        for (i in size - 1 downTo 0) {
            if (sign[i] == 1) {
                sign[i] = 0     //对取到的View的标志位置0
                return i
            }
        }
        return null
    }

    companion object {

        private val giftId = listOf(
            R.drawable.ic_gift1,
            R.drawable.ic_gift2,
            R.drawable.ic_gift3,
            R.drawable.ic_gift4,
            R.drawable.ic_gift5,
            R.drawable.ic_gift6,
            R.drawable.ic_gift7,
            R.drawable.ic_gift8
        )
    }
}