package com.Meteors.android.meteors.ui.ShortVideo.assets

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.Meteors.android.meteors.MediaPlayerPool
import com.Meteors.android.meteors.logic.model.VideoResponse
import com.Meteors.android.meteors.logic.network.Repository

class AssetsFragmentViewModel: ViewModel() {

    lateinit var mediaPlayerPool: MediaPlayerPool       //MediaPlayer的集中管理工具类

    private val commentsId = MutableLiveData<String>()

    val comments = Transformations.switchMap(commentsId){
        Repository.getComments(commentsId.value!!)
    }

    //初始化mediaPlayerPool，如果未初始化就进行实例化
    fun initMediaPlayerPool(
        context: Context,
        list: List<VideoResponse>,
        sourceType: Int,
        windowWidth: Int
    ): MediaPlayerPool {
        if (!this::mediaPlayerPool.isInitialized) {
            mediaPlayerPool = MediaPlayerPool(context, list, sourceType, windowWidth)
        }
        return mediaPlayerPool
    }

    //判断mediaPlayerPool是否实例化
    fun mediaPlayerIsInitialiazed(): Boolean {
        return this::mediaPlayerPool.isInitialized
    }

    //获取评论的接口，向仓库获取评论数据
    fun getComments(videoId: String){
        commentsId.value = videoId
    }

    //释放mediaPlayerPool
    override fun onCleared() {
        super.onCleared()
        mediaPlayerPool.release()
    }
}