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
private const val TAG1 = "Test"

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var mediaPlayerPool: MediaPlayerPool

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)


        val pagerSnapHelper = MyPagerSnapHelper()
        val videoUri =
            listOf("VID_1.mp4", "VID_2.mp4", "VID_3.mp4", "VID_4.mp4", "VID_5.mp4", "VID_6.mp4")

        mediaPlayerPool = MediaPlayerPool(this, videoUri)

        var videoAdapter = VideoAdapter(videoUri)
        viewBinding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = videoAdapter
            pagerSnapHelper.attachToRecyclerView(this)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
        RecyclerView.ViewHolder(itemBinding.root), View.OnClickListener, SurfaceHolder.Callback {

        private lateinit var mediaPlayer: MediaPlayer
        private var curPosition = 0
        private val holder: SurfaceHolder = itemBinding.video.holder
        private var isInitialized = false

        @RequiresApi(Build.VERSION_CODES.N)
        fun onBind(position: Int, videoUri: String) {
            Log.d(TAG, "viewHolder($position) onBind()")
            this.curPosition = position
            mediaPlayerPool.load(position)
            itemBinding.root.setOnClickListener(this)
            holder.addCallback(this)
        }


        fun videoStart() {

        }

        fun videoPause() {

        }

        override fun onClick(v: View?) {
            when (v?.id) {

                R.id.btn_praise -> {

                }
                R.id.btn_comment -> {

                }
                else -> {
                    if (mediaPlayerPool.isPaused()) {
                        startVideo()
                        itemBinding.btnPause.visibility = View.INVISIBLE
                    } else {
                        mediaPlayerPool.pauseVideo()
                        itemBinding.btnPause.visibility = View.VISIBLE
                    }
                }
            }
        }

        fun startVideo() {
            mediaPlayerPool.startVideo(curPosition, holder, isInitialized)
            Log.d(TAG1, "Holder-${curPosition} startVideo()")
        }

        override fun surfaceCreated(holder: SurfaceHolder) {
            Log.d(TAG1, "surfaceCreated")
            mediaPlayerPool.surfaceCreatedFinished(curPosition, holder)
            isInitialized = true
        }

        override fun surfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {}
    }

    private inner class VideoAdapter(val videoList: List<String>) :
        RecyclerView.Adapter<VideoHolder>() {

        private var curHolder = HashSet<VideoHolder>()      //用于保存当前屏幕上的VideoHolder

        private var scrollState = 0     //用于判断滚动状态

        private var isFirstBind = true     //判断是否为第一次执行onBind()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoHolder {
            val itemBinding = VideoItemBinding.inflate(layoutInflater, parent, false)
            return VideoHolder(itemBinding)
        }

        @RequiresApi(Build.VERSION_CODES.N)
        override fun onBindViewHolder(holder: VideoHolder, position: Int) {
            val videoUri = videoList[position]
            holder.onBind(position, videoUri)
            if (isFirstBind) {
                holder.startVideo()
                isFirstBind = false
            }
        }

        override fun getItemCount(): Int {
            return videoList.size
        }

        override fun onViewRecycled(holder: VideoHolder) {
            super.onViewRecycled(holder)
        }

        /**
         * 新的VideoHolder出新在屏幕上
         */
        override fun onViewAttachedToWindow(holder: VideoHolder) {
            super.onViewAttachedToWindow(holder)
            curHolder.add(holder)
            holder.videoStart()
        }

        /**
         * VideoHolder滑出屏幕
         */
        override fun onViewDetachedFromWindow(holder: VideoHolder) {
            super.onViewDetachedFromWindow(holder)
            curHolder.remove(holder)
            holder.videoPause()
        }

        /**
         * 对RecyclerView的滑动事件进行监听
         * 当滑动之后页面不变的情况下，SCROLL_STATE_IDLE会进行两次回调，中间发生一次SCROLL_STATE_SETTLING
         * scrollState用来判断滚动事件的结束，我们只要最后一次SCROLL_STATE_IDLE
         */
        fun onScrollStateChanged(newState: Int) {
            when (newState) {
                RecyclerView.SCROLL_STATE_IDLE -> {
                    if (scrollState == 1) {
                        curHolder.first().startVideo()
                        scrollState = 0
                    }
                }
                RecyclerView.SCROLL_STATE_SETTLING -> {
                    scrollState++
                }
            }
        }

    }

    private class MyPagerSnapHelper : PagerSnapHelper() {}
}