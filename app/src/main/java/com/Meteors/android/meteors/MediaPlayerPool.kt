package com.Meteors.android.meteors

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import android.util.LruCache
import android.view.SurfaceHolder
import android.widget.Toast
import com.Meteors.android.meteors.logic.model.VideoResponse
import java.lang.Exception

private const val TAG = "Meteors_MediaPlayerPool"

class MediaPlayerPool(
    private val context: Context,
    private val list: List<VideoResponse>,
    private val sourceType: Int,
    private val windowWidth: Int
) {
    companion object{
        const val SOURCE_NET = 1        //视频来源为网络
        const val SOURCE_ASSETS = 2     //视频来源为assets目录
    }
    private val mLruCache = MLruCache(5)
    private val preCacheCount = 2
    private var player: MediaPlayer? = null
    private var curPosition = -1

    fun load(position: Int) {
        //创建缓存
        for (i in 0..preCacheCount) {
            if (position + i < list.size) {
                mediaPlayerInstance(position + i)
            }
        }
        for(i in 1..preCacheCount){
            if (position - i >= 0){
                mediaPlayerInstance((position - i))
            }
        }
    }

    fun startVideo(position: Int, surfaceHolder: SurfaceHolder, holderInitialized: Boolean) {
        if (curPosition != position) {
            player?.pause()
        }
        curPosition = position

        if (!holderInitialized) {
            return
        }

        player = mediaPlayerInstance(position)
        player!!.setDisplay(surfaceHolder)

        if (!mLruCache.initMap.containsKey(position)) {
            player!!.setOnPreparedListener {
                Log.d(TAG, "player-$position prepared()")
                val multiple = windowWidth * 1.0 / player!!.videoWidth   //根据屏幕宽度获取伸缩系数
                val newHeight = (multiple * player!!.videoHeight).toInt()
                surfaceHolder.setFixedSize(windowWidth, newHeight)     //为Surface设置新的宽高
                player!!.start()
                mLruCache.initMap[position] = true
            }
        } else {
            val multiple = windowWidth * 1.0 / player!!.videoWidth
            val newHeight = (multiple * player!!.videoHeight).toInt()
            surfaceHolder.setFixedSize(windowWidth, newHeight)
            player!!.start()
        }

    }

    fun pauseVideo() {
        if (mLruCache.initMap.containsKey(curPosition)) {
            if (player!!.isPlaying) {
                player!!.pause()
            }
        }
    }

    fun resumeVideo() {
        if (mLruCache.initMap.containsKey(curPosition)) {
            if (!player!!.isPlaying) {
                player!!.start()
            }
        }
    }

    fun isPaused(): Boolean {
        player?.apply {
            return !isPlaying
        }
        return true
    }

    //清除缓存
    fun release(){
        player?.pause()
        mLruCache.evictAll()
    }

    fun restart() {
        player?.seekTo(0)
    }

    private fun mediaPlayerInstance(position: Int): MediaPlayer {
        var player: MediaPlayer? = mLruCache.get(position)

        if (player == null) {
            player = MediaPlayer()
            mLruCache.put(position, player)
            try {
                when(sourceType){
                    SOURCE_ASSETS -> {
                        val fd = context.assets.openFd("${list[position].id}.mp4")
                        player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                    }
                    SOURCE_NET -> {
                        player.setDataSource(context, list[position].uri)
                    }
                    else -> {
                        throw Throwable("视频来源不存在。")
                    }
                }
                player.prepareAsync()
                player.isLooping = true
                player.setOnPreparedListener {
                    Log.d(TAG, "player-$position prepared()")
                    mLruCache.initMap[position] = true
                }
            } catch (e: Exception) {
                Toast.makeText(context, "视频-${list[position].id} 加载失败", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        return player
    }

    private class MLruCache(maxSize: Int) : LruCache<Int, MediaPlayer>(maxSize) {
        val initMap = HashMap<Int, Boolean>()
        override fun entryRemoved(
            evicted: Boolean,
            key: Int?,
            oldValue: MediaPlayer?,
            newValue: MediaPlayer?
        ) {
            super.entryRemoved(evicted, key, oldValue, newValue)
            Log.d(TAG, "mediaPlayer-$key has been evicted()")
            oldValue?.release()
            initMap.remove(key)
        }
    }

}