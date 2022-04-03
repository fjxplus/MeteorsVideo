package com.Meteors.android.meteors.logic.network

import android.util.Log
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers

private const val TAG = "Meteors_Repository"
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
            Log.d(TAG, "Repository无法获取VideoList")
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
            Log.d(TAG, "Repository无法获取Video")
            Result.failure(e)
        }
        emit(result)
    }

    //获取评论区列表
    fun getComments(videoId: String) = liveData(Dispatchers.IO) {
        val result = try {
            val commentList = VideoNetwork.getComment(videoId)
            Result.success(commentList)
        }catch (e: Exception){
            Log.d(TAG, "Repository无法获取Comment")
            Result.failure(e)
        }
        emit(result)
    }

    //获取用户头像URL
    fun getUserImageUrl(userId: String): String{
        return "${ServiceCreator.BASE_URL}image/user/$userId.jpg"
    }
}