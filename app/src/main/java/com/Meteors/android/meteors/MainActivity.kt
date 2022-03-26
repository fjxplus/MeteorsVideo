package com.Meteors.android.meteors

import android.content.res.AssetManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.nfc.Tag
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.Meteors.android.meteors.databinding.ActivityMainBinding
import com.Meteors.android.meteors.databinding.VideoItemBinding
import java.lang.Exception

private const val TAG = "VideoAdapter"

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val pagerSnapHelper = MyPagerSnapHelper()
        var videoAdapter = VideoAdapter(listOf("VID_1.mp4", "VID_2.mp4", "VID_3.mp4", "VID_4.mp4", "VID_5.mp4", "VID_6.mp4"))
        viewBinding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = videoAdapter
            pagerSnapHelper.attachToRecyclerView(this)
            addOnScrollListener(object : RecyclerView.OnScrollListener(){
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    videoAdapter.onScrollStateChanged(newState)
                }
            })
        }

        viewBinding.refresh.setOnRefreshListener {
            Log.d(TAG, "refresh")
            var sum = 0.0
            for (i in 0 until 10000) {
                sum += i
            }
            viewBinding.refresh.isRefreshing = false
        }

    }

    private inner class VideoHolder(val itemBinding: VideoItemBinding) :
        RecyclerView.ViewHolder(itemBinding.root), SurfaceHolder.Callback, View.OnClickListener {

        private lateinit var mediaPlayer: MediaPlayer
        private lateinit var videoUri: String
        private val holder: SurfaceHolder = itemBinding.video.holder
        private var isInitialized = false

        @RequiresApi(Build.VERSION_CODES.N)
        fun onBind(videoUri: String) {
            Log.d(TAG, "viewHolder onBind(), Uri is $videoUri")
            this.videoUri = videoUri
            itemBinding.root.setOnClickListener(this)
            holder.addCallback(this)
        }

        fun initMediaPlayer(){
            mediaPlayer = MediaPlayer()
            isInitialized = true
            try {
                val fd = assets.openFd(videoUri)
                mediaPlayer.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)

                mediaPlayer.prepareAsync()
            }catch (e: Exception){
                Toast.makeText(this@MainActivity, "视频加载失败", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }

        }

        fun videoStart(){
            if(!isInitialized){
                initMediaPlayer()
            }else{
                mediaPlayer.start()
            }
            itemBinding.btnPause.visibility = View.INVISIBLE
        }

        fun videoPause(){
            if (mediaPlayer.isPlaying){
                mediaPlayer.pause()
                itemBinding.btnPause.visibility = View.VISIBLE
            }
        }

        fun releaseMediaPlayer() {
            Log.d(TAG, "mediaPlayer release()")
            if(isInitialized){
                mediaPlayer.release()
                isInitialized = false
            }
        }

        override fun surfaceCreated(holder: SurfaceHolder) {
            mediaPlayer.setDisplay(holder)
            mediaPlayer.setOnPreparedListener {
                Log.d(TAG, "surfaceCreated and mediaPlayer prepared()")
                videoStart()
            }
            mediaPlayer.setOnCompletionListener {
                itemBinding.btnPause.visibility = View.VISIBLE
            }
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
        }

        override fun onClick(v: View?) {
            when(v?.id){

                R.id.btn_praise ->{

                }
                R.id.btn_comment ->{

                }
                else -> {
                    if(mediaPlayer.isPlaying){
                        videoPause()
                    }else{
                        videoStart()
                    }
                }
            }
        }
    }

    private inner class VideoAdapter(val videoList: List<String>) :
        RecyclerView.Adapter<VideoHolder>()  {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoHolder {
            val itemBinding = VideoItemBinding.inflate(layoutInflater, parent, false)
            return VideoHolder(itemBinding)
        }

        @RequiresApi(Build.VERSION_CODES.N)
        override fun onBindViewHolder(holder: VideoHolder, position: Int) {
            val videoUri = videoList[position]
            holder.onBind(videoUri)
        }

        override fun getItemCount(): Int {
            return videoList.size
        }

        override fun onViewRecycled(holder: VideoHolder) {
            super.onViewRecycled(holder)
            holder.releaseMediaPlayer()
            Log.d(TAG, "holder onViewRecycled()")
        }

        override fun onViewAttachedToWindow(holder: VideoHolder) {
            super.onViewAttachedToWindow(holder)
            holder.videoStart()
            Log.d(TAG, "holder attachedToWindow()")
        }

        override fun onViewDetachedFromWindow(holder: VideoHolder) {
            super.onViewDetachedFromWindow(holder)
            Log.d(TAG, "holder detachedToWindow()")
            holder.videoPause()
        }

        fun onScrollStateChanged(newState: Int){
            when(newState){
                RecyclerView.SCROLL_STATE_IDLE -> {
                    Log.d(TAG, "滑动停止")
                }

            }
        }

    }

    private class MyPagerSnapHelper : PagerSnapHelper() {

    }
}