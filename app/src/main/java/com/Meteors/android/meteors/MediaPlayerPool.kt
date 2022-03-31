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

/**
 * @Description: 管理者一个存储MediaPlayer的LruCache，进行LRU缓存机制，对弹出的MediaPlayer进行资源释放
 * 具有加载MediaPlayer，开始播放，暂停播放，释放所有资源的功能
 * @Param:  Context, 视频信息列表，视频来源类型，屏幕宽度
 */
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
    private val mLruCache = MLruCache(7)
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

    /**
    * @Description:  开始播放视频，如果SurfaceView已经加载完毕，将MediaPlayer和SurfaceView进行绑定
    * @Param:  position:视频下标索引, surfaceHolder: 和Player进行绑定, holderInitialized: SurfaceView是否加载完成
    * @return:  void
    */
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

    //暂停视频
    fun pauseVideo() {
        if (mLruCache.initMap.containsKey(curPosition)) {
            if (player!!.isPlaying) {
                player!!.pause()
            }
        }
    }

    //开始播放暂停的视频
    fun resumeVideo() {
        if (mLruCache.initMap.containsKey(curPosition)) {       //判断当前实例player是否已经初始化
            if (!player!!.isPlaying) {
                player!!.start()
            }
        }
    }

    //暂停当前播放视频
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

    /**
    * @Description: 创建MediaPlayer实例的方法
    * @Param:  视频下标
    * @return:  MediaPlayer
    */
    private fun mediaPlayerInstance(position: Int): MediaPlayer {
        var player: MediaPlayer? = mLruCache.get(position)  //如果缓存中实例命中，则返回已有的实例

        if (player == null) {       //不存在就创建
            player = MediaPlayer()
            mLruCache.put(position, player)
            try {
                when(sourceType){       //根据不同类型的文件来源进行不同的路径设置，分为assets目录资源和网络Url
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
                player.prepareAsync()       //后台加载
                player.isLooping = true
                player.setOnPreparedListener {
                    Log.d(TAG, "player-$position prepared()")
                    mLruCache.initMap[position] = true      //加载完毕在map中进行标记加载完成
                }
            } catch (e: Exception) {
                Toast.makeText(context, "视频-${list[position].id} 加载失败", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        return player
    }


    /**
    * @Description:  重写entryRemoved()方法，对移除的MediaPlayer进行资源的释放
    * @Param:  LruCache最大容量
    */
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