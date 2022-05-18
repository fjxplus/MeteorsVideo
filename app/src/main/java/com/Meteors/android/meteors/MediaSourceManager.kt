package com.Meteors.android.meteors

import android.media.MediaDataSource
import android.os.Build
import android.util.Log
import android.util.LruCache
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.Meteors.android.meteors.logic.model.VideoResponse
import com.Meteors.android.meteors.logic.network.Repository

private const val TAG = "Meteors_MediaPlayerPool"

/**
 * @Description: 管理者一个存储MediaPlayer的LruCache，进行LRU缓存机制，对弹出的MediaPlayer进行资源释放
 * 具有加载MediaPlayer，开始播放，暂停播放，释放所有资源的功能
 * @Param:  Context, 视频信息列表，视频来源类型，屏幕宽度
 */
class MediaSourceManager(
    private val lifecycleOwner: LifecycleOwner
    //private val list: List<VideoResponse>
) {

    companion object {
        const val SOURCE_NET = 1        //视频来源为网络
        const val SOURCE_ASSETS = 2     //视频来源为assets目录
    }

    private val mLruCache = MLruCache(7)

    /**
     * 加载DataSource, 由外部控制预加载
     */
    fun load(videoID: String, callback: CallBack) {
        var dataSource: DataSource? = mLruCache.get(videoID)
        if (dataSource != null) {
            callback.onSuccess(videoID, dataSource)
        } else {
            if (!mLruCache.initSet.contains(videoID)) {        //没有加载过
                val videoLiveData = Repository.getVideo(videoID)
                mLruCache.initSet.add(videoID)
                videoLiveData.observe(lifecycleOwner, Observer { result ->
                    val videoBytes = result.getOrNull()
                    if (videoBytes != null) {
                        Log.d("Meteors_VideoAdapter2", "get stream success")
                        dataSource = DataSource(videoBytes)
                        mLruCache.put(videoID, dataSource)
                        callback.onSuccess(videoID, dataSource!!)
                    } else {
                        Log.d("Meteors_VideoAdapter2", "get stream fail")
                        mLruCache.initSet.remove(videoID)
                        result.exceptionOrNull()?.printStackTrace()
                        callback.onFailure(videoID)
                    }
                })
            } else {
                //已经在加载并且监听
            }
        }
    }

    fun release(){
        mLruCache.evictAll()
    }

    interface CallBack {
        fun onSuccess(videoID: String, dataSource: DataSource)
        fun onFailure(videoID: String)
    }

    /**
     * @Description:  重写entryRemoved()方法，对移除的MediaPlayer进行资源的释放
     * @Param:  LruCache最大容量
     */
    private class MLruCache(maxSize: Int) : LruCache<String, DataSource>(maxSize) {
        val initSet = HashSet<String>()

        override fun entryRemoved(
            evicted: Boolean,
            key: String?,
            oldValue: DataSource?,
            newValue: DataSource?
        ) {
            super.entryRemoved(evicted, key, oldValue, newValue)
            oldValue?.close()
            initSet.remove(key)
        }
    }

    class DataSource(private val videoBytes: ByteArray) : MediaDataSource() {
        override fun close() {
        }

        override fun readAt(position: Long, buffer: ByteArray?, offset: Int, size: Int): Int {
            if (size == 0) {
                return 0
            }
            var pos = position.toInt()
            for (i in 0 until size) {
                if (pos == videoBytes.size) {
                    return -1
                }
                buffer?.set(offset + i, videoBytes[pos])
                pos++
            }
            return size
        }

        override fun getSize(): Long {
            return videoBytes.size.toLong()
        }
    }

}