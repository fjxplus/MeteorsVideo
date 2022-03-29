package com.Meteors.android.meteors.logic.model

import android.net.Uri
import com.Meteors.android.meteors.logic.network.ServiceCreator
import com.google.gson.annotations.SerializedName

data class VideoListResponse(val videos: List<VideoResponse>)

data class VideoResponse(
    val id: String,
    @SerializedName("owner_id") val ownerId: String,
    @SerializedName("ad_txt") val adTxt: String
){
    val uri: Uri get() = Uri.parse("${ServiceCreator.BASE_URL}/video/$id.mp4")
}

data class CommentListResponse(val commentList: List<Comment>)

data class Comment(val id: String, val txt: String)
