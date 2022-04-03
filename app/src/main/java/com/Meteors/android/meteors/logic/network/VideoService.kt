package com.Meteors.android.meteors.logic.network

import com.Meteors.android.meteors.logic.model.CommentListResponse
import com.Meteors.android.meteors.logic.model.VideoListResponse
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface VideoService {

    @GET("video_list.json")
    fun getVideoList():Call<VideoListResponse>

    @GET("video/{id}.mp4")
    fun getVideo(@Path("id") id: String):Call<ResponseBody>

    @GET("video/comment/comments_{id}.json")
    fun getComments(@Path("id") videoId: String):Call<CommentListResponse>

}