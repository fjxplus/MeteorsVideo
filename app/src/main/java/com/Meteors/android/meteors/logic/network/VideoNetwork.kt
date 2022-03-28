package com.Meteors.android.meteors.logic.network

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * 网络数据源访问入口
 */
object VideoNetwork {

    private val videoService = ServiceCreator.create<VideoService>()

    suspend fun getVideoList() = videoService.getVideoList().await()

    suspend fun getVideo(id: String) = videoService.getVideo(id).await()

    suspend fun getComment(id: String) = videoService.getComment(id).await()

    //重写Call类型的await()方法，对响应的异常进行统一处理
    private suspend fun <T> Call<T>.await(): T{
        return suspendCoroutine { continuation ->
            enqueue(object : Callback<T>{
                override fun onResponse(call: Call<T>, response: Response<T>) {
                    val body = response.body()
                    if(body != null){
                        continuation.resume(body)
                    }else{
                        continuation.resumeWithException(RuntimeException("响应超时"))
                    }
                }

                override fun onFailure(call: Call<T>, t: Throwable) {
                    continuation.resumeWithException(t)
                }

            })
        }
    }
}