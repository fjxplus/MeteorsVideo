package com.Meteors.android.meteors.logic.network

import android.util.Log
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream

private const val TAG = "Meteors_Repository"
/**
* @Description: 仓库类，程序的数据源
*/
object Repository {

    /**
    * @Description: 使用liveData开启协程域，可以根据数据的返回值进行不同异常的响应
    * @return:  LiveData
    */
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


    /**
    * @Description: 获取视频流文件
    */
    fun getVideo(id: String) = liveData(Dispatchers.IO) {
        val result = try {
            //获取视频的字节码数组
            val videoBytes = VideoNetwork.getVideo(id).bytes()
            Result.success(videoBytes)
        }catch (e: Exception){
            Log.d(TAG, "Repository无法获取Video")
            Result.failure(e)
        }
        emit(result)
    }

    /**
    * @Description: 获取评论区列表
    * @Param: 视频ID
    * @return: LiveData
    */
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

    /**
    * @Description: 获取用户头像的URL地址
    * @Param: 用户Id
    */
    fun getUserImageUrl(userId: String): String{
        return "${ServiceCreator.BASE_URL}image/user/$userId.jpg"
    }
}