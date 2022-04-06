package com.Meteors.android.meteors.logic.model

import android.net.Uri
import com.Meteors.android.meteors.logic.network.ServiceCreator
import com.google.gson.annotations.SerializedName

data class VideoListResponse(val videos: List<VideoResponse>)

data class VideoResponse(
    val id: String,
    @SerializedName("owner_id") val ownerId: String,
    @SerializedName("owner_name") val ownerName: String,
    @SerializedName("ad_txt") val adTxt: String
) {
    val uri: Uri get() = Uri.parse("${ServiceCreator.BASE_URL}/video/$id.mp4")
}

data class CommentListResponse(
    @SerializedName("video_id") val videoId: String,
    val comments: List<Comment>
)

data class Comment(
    @SerializedName("user_id") val userId: String,
    @SerializedName("user_name") val userName: String,
    val txt: String
)
