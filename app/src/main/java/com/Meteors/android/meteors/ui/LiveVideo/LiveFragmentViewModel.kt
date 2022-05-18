package com.Meteors.android.meteors.ui.LiveVideo

import android.os.Handler
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.Meteors.android.meteors.logic.model.Comment
import com.Meteors.android.meteors.logic.thread.CommentLoader

class LiveFragmentViewModel: ViewModel() {

    val comments = ArrayList<Comment>()

    val updateSignal = MutableLiveData<Any>()

    lateinit var commentLoader: CommentLoader

    /**
    * @Description: 初始化CommentLoader并开始获取评论
    * @Param: 主线程Handler
    */
    fun initCommentLoader(updateHandler: Handler){
        commentLoader = CommentLoader(comments, updateHandler)
        commentLoader.start()
    }

    fun startLoading() {
        commentLoader.startLoading()
    }

    fun stopLoading() {
        commentLoader.stopLoading()
    }

    /**
    * @Description: 清除CommentLoader线程任务，回收资源
    */
    override fun onCleared() {
        super.onCleared()
        commentLoader.quit()
    }

}