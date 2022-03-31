package com.Meteors.android.meteors.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import com.Meteors.android.meteors.MediaPlayerPool
import com.Meteors.android.meteors.logic.model.VideoResponse

class AssetsFragmentViewModel: ViewModel() {

    lateinit var mediaPlayerPool: MediaPlayerPool       //MediaPlayer的集中管理工具类

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

    //释放mediaPlayerPool
    override fun onCleared() {
        super.onCleared()
        mediaPlayerPool.release()
    }
}