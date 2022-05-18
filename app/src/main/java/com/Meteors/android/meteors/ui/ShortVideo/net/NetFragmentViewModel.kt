package com.Meteors.android.meteors.ui

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.Meteors.android.meteors.MediaPlayerPool
import com.Meteors.android.meteors.logic.model.CommentListResponse
import com.Meteors.android.meteors.logic.model.VideoResponse
import com.Meteors.android.meteors.logic.network.Repository

class NetFragmentViewModel : ViewModel() {

    private val videoListLiveData = MutableLiveData<Any>()

    val videos = ArrayList<VideoResponse>()

    val videoList = Transformations.switchMap(videoListLiveData) {
        Repository.getVideoList()
    }

    private val commentsId = MutableLiveData<String>()

    val comments = Transformations.switchMap(commentsId){
        Repository.getComments(commentsId.value!!)
    }

    init {
        getVideoList()
    }

    //刷新，重新获取视频列表，对原有数据进行清除
    fun refresh(){
        videos.clear()
        getVideoList()
    }

    //获取视频接口，触发LiveData转换，向网络层获取视频
    fun getVideoList() {
        videoListLiveData.value = videoListLiveData.value
    }

    //获取评论的接口，向仓库获取评论数据
    fun getComments(videoId: String){
        commentsId.value = videoId
    }

    //释放mediaPlayerPool
    override fun onCleared() {
        super.onCleared()
    }
}