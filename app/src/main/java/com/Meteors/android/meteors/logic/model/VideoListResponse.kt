package com.Meteors.android.meteors.logic.model

import com.google.gson.annotations.SerializedName

data class VideoListResponse(val videos: List<VideoResponse>)

data class VideoResponse(
    val id: String,
    @SerializedName("owner_id") val ownerId: String,
    @SerializedName("ad_txt") val adTxt: String
)

data class CommentListResponse(val commentList: List<Comment>)

data class Comment(val id: String, val txt: String)
