package com.Meteors.android.meteors

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import android.util.LruCache
import android.view.SurfaceHolder
import android.widget.Toast
import java.lang.Exception

private const val TAG = "Meteors_MediaPlayerPool"

class MediaPlayerPool(private val context: Context, private val list: List<String>, private val windowWidth: Int) {
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
                val multiple = windowWidth * 1.0/ player!!.videoWidth
                val newHeight = (multiple * player!!.videoHeight).toInt()
                surfaceHolder.setFixedSize(windowWidth,  newHeight)
                Log.d(TAG, ", originWidth = ${player!!.videoWidth}, nowSize = $windowWidth, originHeight = ${player!!.videoHeight * multiple}," +
                        "now = ${player!!.videoHeight * multiple}")
                player!!.start()
                mLruCache.initMap[position] = true
            }
        } else {
            val multiple = windowWidth * 1.0/ player!!.videoWidth
            val newHeight = (multiple * player!!.videoHeight).toInt()
            surfaceHolder.setFixedSize(windowWidth,  newHeight)
            player!!.start()
        }

    }

    fun pauseVideo() {
        if(mLruCache.initMap.containsKey(curPosition)){
            if(player!!.isPlaying){
                player!!.pause()
            }else{
                player?.start()
            }
        }
    }

    fun isPaused(): Boolean {
        player?.apply {
            return !isPlaying
        }
        return true
    }

    fun restart(){
        player?.seekTo(0)
    }

    private fun mediaPlayerInstance(position: Int): MediaPlayer {
        var player: MediaPlayer? = mLruCache.get(position)

        if (player == null) {
            player = MediaPlayer()
            mLruCache.put(position, player)
            try {
                val fd = context.assets.openFd(list[position])
                player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)

                player.prepareAsync()
                player.isLooping = true
                player.setOnPreparedListener {
                    Log.d(TAG, "player-$position prepared()")
                    mLruCache.initMap[position] = true
                }
            } catch (e: Exception) {
                Toast.makeText(context, "视频-${list[position]} 加载失败", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        return player
    }


    /*
    private fun releasePlayer(){
        Log.d(TAG, "mediaPlayer release()")
        if(isInitialized){
            mediaPlayer.release()
            isInitialized = false
        }
    }

     */

    private class MLruCache(maxSize: Int): LruCache<Int, MediaPlayer>(maxSize){
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