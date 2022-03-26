package com.Meteors.android.meteors

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import android.util.LruCache
import android.view.SurfaceHolder
import android.widget.Toast
import java.lang.Exception

private const val TAG = "VideoAdapter"
private const val TAG1 = "Test"
class MediaPlayerPool(private val context: Context, private val list: List<String>) {
    private val mLruCache = LruCache<Int, MediaPlayer>(5)
    private val preCacheCount = 2
    private var player: MediaPlayer? = null
    private var curPosition = -1
    private val hashmap = HashMap<Int, Boolean>()

    fun load(position: Int) {
        val player = mediaPlayerInstance(position)
        //创建缓存
        for (i in 1..preCacheCount) {
            if (position + i < list.size) {
                mediaPlayerInstance(position + i)
            }
        }
    }

    fun startVideo(position: Int, surfaceHolder: SurfaceHolder, holderInitialized: Boolean){
        pauseVideo()
        Log.d(TAG1, "curPosition is $curPosition, position is $position, $holderInitialized")

        curPosition = position

        if(holderInitialized){
            player = mediaPlayerInstance(position)
            player!!.setDisplay(surfaceHolder)
            //出问题
            if(hashmap[position] == false){
                player!!.setOnPreparedListener{
                    Log.d(TAG1, "player prepared()")
                    player!!.start()
                }
            }else{
                player!!.start()
            }

        }
    }

    fun surfaceCreatedFinished(position: Int, surfaceHolder: SurfaceHolder){
        Log.d(TAG1, "curPosition is $curPosition, position is $position")

            startVideo(position, surfaceHolder, true)

    }

    fun pauseVideo(){
        player?.pause()
    }

    fun isPaused(): Boolean{
        player?.apply {
            return !isPlaying
        }
        return true
    }

    private fun mediaPlayerInstance(position: Int): MediaPlayer {
        var player: MediaPlayer? = mLruCache.get(position)

        if (player == null) {
            player = MediaPlayer()
            try {
                val fd = context.assets.openFd(list[position])
                player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                player.prepareAsync()
                player.setOnPreparedListener{
                    Log.d(TAG1, "player prepared()")
                    hashmap[position] = true
                }
            } catch (e: Exception) {
                Toast.makeText(context, "视频-${list[position]} 加载失败", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }

            mLruCache.put(position, player)
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


}