package com.Meteors.android.meteors.logic.network

import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "Meteors_Network"
/**
 * 网络数据源访问入口
 */
object VideoNetwork {

    private val videoService = ServiceCreator.create<VideoService>()

    suspend fun getVideoList() = videoService.getVideoList().await()

    suspend fun getVideo(id: String) = videoService.getVideo(id).await()

    suspend fun getComment(videoId: String) = videoService.getComments(videoId).await()

    //重写Call类型的await()方法，对响应的异常进行统一处理
    private suspend fun <T> Call<T>.await(): T{
        return suspendCoroutine { continuation ->
            enqueue(object : Callback<T>{
                override fun onResponse(call: Call<T>, response: Response<T>) {
                    val body = response.body()
                    if(body != null){
                        continuation.resume(body)
                    }else{
                        Log.d(TAG, "VideoNetwork, Call响应数据为空")
                        continuation.resumeWithException(RuntimeException("VideoNetwork响应超时"))
                    }
                }

                override fun onFailure(call: Call<T>, t: Throwable) {
                    Log.d(TAG, "VideoNetwork, Call执行失败")
                    continuation.resumeWithException(t)
                }

            })
        }
    }
}