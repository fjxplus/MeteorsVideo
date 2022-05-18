package com.Meteors.android.meteors

import android.annotation.SuppressLint
import android.media.MediaDataSource
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Meteors.android.meteors.databinding.ActivityMain2Binding
import com.Meteors.android.meteors.logic.model.VideoResponse
import com.Meteors.android.meteors.logic.network.Repository
import com.Meteors.android.meteors.ui.ShortVideo.recyclerView.VideoAdapter
import com.Meteors.android.meteors.ui.ShortVideo.recyclerView.VideoAdapter2
import java.io.InputStream

class MainActivity2 : AppCompatActivity() {
    private lateinit var binding: ActivityMain2Binding
    private lateinit var adapter2: VideoAdapter2
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val videoList = listOf(
            VideoResponse("VID_1", "user5", "@吕子乔", "我早就说过：人生苦短，及时行乐才是王道。"),
            VideoResponse("VID_2", "user6", "@吕子乔", "我早就说过：人生苦短，及时行乐才是王道。"),
            VideoResponse("VID_3", "user7", "@吕子乔", "我早就说过：人生苦短，及时行乐才是王道。"),
            VideoResponse("VID_4", "user8", "@吕子乔", "我早就说过：人生苦短，及时行乐才是王道。")
        )
        val windowWidth = windowManager.currentWindowMetrics.bounds.width()
        val mediaResourceManager = MediaSourceManager(this)
        adapter2 = VideoAdapter2(this, videoList, mediaResourceManager, windowWidth){ videoID ->
            //向ViewModel请求评论区数据
            //viewModel.getComments(videoId)
        }
        binding.recyclerView.adapter = adapter2
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        //监听滑动状态
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                adapter2.onScrollStateChanged(newState)
            }
        })
        val pagerSnapHelper =
            VideoAdapter2.MyPagerSnapHelper()
        pagerSnapHelper.attachToRecyclerView(binding.recyclerView)

    }

}