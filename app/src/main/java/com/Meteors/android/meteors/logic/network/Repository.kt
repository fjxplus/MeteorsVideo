package com.Meteors.android.meteors.logic.network

import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers

/**
 * 仓库类
 */
object Repository {

    //使用liveData开启协程域，可以根据数据的返回值进行不同异常的响应
    fun getVideoList() = liveData(Dispatchers.IO) {
        val result = try {
            val videoListResponse = VideoNetwork.getVideoList()
            val videos = videoListResponse.videos
            Result.success(videos)
        }catch (e: Exception){
            Result.failure(e)
        }
        emit(result)
    }

    //解析视频字节流，返回可播放的视频文件（未完成）
    fun getVideo(id: String) = liveData(Dispatchers.IO) {
        val result = try {
            val videoStream = VideoNetwork.getVideo(id).byteStream()
            //对视频文件流进行读取
            Result.success(videoStream)
        }catch (e: Exception){
            Result.failure(e)
        }
        emit(result)
    }

    //获取评论区列表
    fun getComment(id: String) = liveData(Dispatchers.IO) {
        val result = try {
            val commentList = VideoNetwork.getComment(id).commentList
            Result.success(commentList)
        }catch (e: Exception){
            Result.failure(e)
        }
        emit(result)
    }
}