package com.Meteors.android.meteors.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.Meteors.android.meteors.logic.model.VideoResponse
import com.Meteors.android.meteors.logic.network.Repository

class NetFragmentViewModel: ViewModel() {
    private val videoListLiveData = MutableLiveData<Any>()

    val videos = ArrayList<VideoResponse>()

    val videoList = Transformations.switchMap(videoListLiveData){
        Repository.getVideoList()
    }

    //获取视频接口，触发LiveData转换，向网络层获取视频
    fun getVideoList(){
        videoListLiveData.value = videoListLiveData.value
    }
}