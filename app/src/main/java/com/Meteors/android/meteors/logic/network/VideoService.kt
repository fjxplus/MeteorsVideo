package com.Meteors.android.meteors.logic.network

import com.Meteors.android.meteors.logic.model.CommentListResponse
import com.Meteors.android.meteors.logic.model.VideoListResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

/**
* @Description: BaseURL:http://47.101.162.133/meteors/ 所有网络接口
*/
interface VideoService {

    @GET("video_list.json")
    fun getVideoList(): Call<VideoListResponse>

    @GET("video/{id}.mp4")
    @Streaming
    fun getVideo(@Path("id") id: String): Call<ResponseBody>

    @GET("video/comment/comments_{videoId}.json")
    fun getComments(@Path("videoId") videoId: String): Call<CommentListResponse>

}