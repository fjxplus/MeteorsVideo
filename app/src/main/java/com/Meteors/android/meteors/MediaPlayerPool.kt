package com.Meteors.android.meteors

import android.content.Context
import android.media.MediaPlayer
import android.util.LruCache
import android.view.SurfaceHolder
import android.widget.Toast
import java.lang.Exception

class MediaPlayerPool(private val context: Context, private val list: List<String>) {
    private val mLruCache = LruCache<Int, MediaPlayer>(5)
    private val preCacheCount = 2

    fun bind(position: Int, surfaceHolder: SurfaceHolder){
        var curPlayer = mLruCache.get(position)
        if(curPlayer == null){
            curPlayer = MediaPlayer()
            try {
                val fd = context.assets.openFd(list[position])
                curPlayer.setDataSource(fd.fileDescriptor)
                curPlayer.prepareAsync()
            }catch (e: Exception){
                Toast.makeText(context, "视频加载失败", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
            mLruCache.put(position, curPlayer)
            //创建缓存
            for (i in 1..preCacheCount){
                if(position + i < list.size){
                    val player = MediaPlayer()
                    try {
                        val fd = context.assets.openFd(list[position])
                        player.setDataSource(fd.fileDescriptor)
                        player.prepareAsync()
                    }catch (e: Exception){
                        Toast.makeText(context, "视频预加载失败", Toast.LENGTH_SHORT).show()
                        e.printStackTrace()
                    }
                    mLruCache.put(position, player)
                }
            }
        }
    }

    fun mediaPlayerConstructor(position: Int): MediaPlayer{
        var player: MediaPlayer = mLruCache.get(position)
        player.duration
        if(player == null){
            val player = MediaPlayer()
            try {
                val fd = context.assets.openFd(list[position])
                player.setDataSource(fd.fileDescriptor)
                player.prepareAsync()
            }catch (e: Exception){
                Toast.makeText(context, "视频-${list[position]} 加载失败", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
            mLruCache.put(position, player)
        }

        return player
    }
}