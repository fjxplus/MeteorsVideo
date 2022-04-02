package com.Meteors.android.meteors.ui

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager

/**
* @Description: 重写LinearLayoutManager的 canScrollVertically()方法，禁止RecyclerView滑动
* @Date: 2022/4/2
*/
class CommentLinearLayoutManager(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : LinearLayoutManager(context, attrs, defStyleAttr, defStyleRes) {
    constructor(context: Context): this(context, null)
    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)
    constructor(context: Context,
                attrs: AttributeSet?,
                defStyleAttr: Int): this(context, attrs, defStyleAttr, 0)
    private var canScrollVertically = true

    /**
    * @Description:  修改是否可滑动的外部方法
    * @Param:  flag: true为可滑动
    */
    fun setCanScrollVertically(flag: Boolean){
        canScrollVertically = flag
    }

    override fun canScrollVertically(): Boolean {
        return canScrollVertically && super.canScrollVertically()
    }
}