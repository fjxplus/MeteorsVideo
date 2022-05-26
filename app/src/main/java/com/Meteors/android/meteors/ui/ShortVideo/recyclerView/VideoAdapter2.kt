package com.Meteors.android.meteors.ui.ShortVideo.recyclerView


import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.Meteors.android.meteors.MediaPlayerPool
import com.Meteors.android.meteors.MediaSourceManager
import com.Meteors.android.meteors.R
import com.Meteors.android.meteors.databinding.VideoItemBinding
import com.Meteors.android.meteors.logic.model.VideoResponse
import com.Meteors.android.meteors.logic.network.Repository
import com.bumptech.glide.Glide

private const val TAG = "Meteors_VideoAdapter2"

/**
 * @Description: RecyclerView适配器，对滚动状态进行监听，在当前屏幕上的VieHolder中进行视频播放
 */
class VideoAdapter2(
    val context: Context,
    private val videoList: List<VideoResponse>,
    private val mediaSourceManager: MediaSourceManager,
    private val windowWidth: Int,
    private val showComments: (videoId: String) -> Unit
) :
    RecyclerView.Adapter<VideoAdapter2.VideoHolder>() {

    private var mediaPlayerPreparing = true

    private var curHolderContainer = HashSet<VideoHolder>()      //用于保存当前屏幕上的VideoHolder

    private val curHolder: VideoHolder get() = curHolderContainer.first()

    private var curPosition = -1

    private var scrollState = 0     //用于判断滚动状态

    private var isWorking = false      //当前是否处于可交互状态，是否处于前台

    val preCacheCount = 2

    private val mediaSourceCallBack = object : MediaSourceManager.CallBack {
        override fun onSuccess(videoID: String, dataSource: MediaSourceManager.DataSource) {
            Log.d(TAG, "mediaSourceCallBack onSuccess(), videoID=$videoID")
            if (videoID == videoList[curPosition].id) {
                mediaPlayer.setDataSource(dataSource)
                mediaPlayer.prepareAsync()
            }
        }

        override fun onFailure(videoID: String) {
            Log.d(TAG, "mediaSourceCallBack onFailure()")
            //处理方法为显示加载失败，点击重新加载
        }

    }

    private val mediaPlayer = MediaPlayer().apply {
        setOnPreparedListener {
            Log.d(TAG, "MediaPlayer prepared()")
            mediaPlayerPreparing = false
            startMediaPlayer()
        }
        setOnCompletionListener {
            start()
        }
        setOnBufferingUpdateListener { mediaPlayer, percent ->
            Log.d(
                "Test",
                "onBufferingUpdate"
            )
        }
    }

    private val surfaceCallBack = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            Log.d(TAG, "surfaceCreated")
            if (curPosition == -1) {
                initMediaPlayer()
            }
            startMediaPlayer()

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

    private fun initMediaPlayer() {
        if (curHolder.adapterPosition != curPosition) {
            Log.d(TAG, "initMediaPlayer")
            mediaPlayer.reset()
            mediaPlayer.setDisplay(curHolder.getSurfaceHolder())
            curPosition = curHolder.adapterPosition
            for (i in 0..preCacheCount) {
                if (curPosition + i < videoList.size) {
                    mediaSourceManager.load(videoList[curPosition + i].id, mediaSourceCallBack)
                }
            }
            for (i in 1..preCacheCount) {
                if (curPosition - i >= 0) {
                    mediaSourceManager.load(videoList[curPosition - i].id, mediaSourceCallBack)
                }
            }
        }
    }

    private fun startMediaPlayer() {
        if (isWorking && !curHolder.getSurfaceViewCreating() && !mediaPlayerPreparing) {
            curHolder.hidePauseSign()
            adjustWindowSize()
            mediaPlayer.start()
        }
    }

    private fun startPlay() {
        Log.d("Test", "startPlay()")
        mediaPlayer.start()

    }

    private fun adjustWindowSize() {
        val multiple = windowWidth * 1.0 / mediaPlayer.videoWidth   //根据屏幕宽度获取伸缩系数
        val newHeight = (multiple * mediaPlayer.videoHeight).toInt()
        curHolder.getSurfaceHolder().setFixedSize(windowWidth, newHeight)     //为Surface设置新的宽高
    }

    /**
     * @Description: 当前Fragment正在交互， isWorking为true，可以播放视频
     */
    fun startWork() {
        isWorking = true
        curHolder.hidePauseSign()
        startMediaPlayer()
    }

    /**
     * @Description: 当前Fragment停止交互，暂停播放视频, isWorking设置为false
     */
    fun stopWork() {
        pauseVideo()
        isWorking = false
    }

    /**
     * @Description: 暂停当前界面视频的播放
     */
    fun pauseVideo() {
        mediaPlayer.pause()
        curHolder.showPauseSign()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoHolder {
        val itemBinding =
            VideoItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoHolder(itemBinding)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onBindViewHolder(holder: VideoHolder, position: Int) {
        val video = videoList[position]
        holder.onBind(position, video)
    }

    /**
     * @Description: payload实现局部刷新
     */
    override fun onBindViewHolder(holder: VideoHolder, position: Int, payloads: MutableList<Any>) {
        super.onBindViewHolder(holder, position, payloads)

    }

    override fun getItemCount(): Int {
        return videoList.size
    }

    /**
     * 新的VideoHolder出新在屏幕上
     */
    override fun onViewAttachedToWindow(holder: VideoHolder) {
        super.onViewAttachedToWindow(holder)
        curHolderContainer.add(holder)
    }

    /**
     * VideoHolder滑出屏幕
     */
    override fun onViewDetachedFromWindow(holder: VideoHolder) {
        super.onViewDetachedFromWindow(holder)
        curHolderContainer.remove(holder)
    }

    /**
     * @Description: RecyclerView的滑动事件进行监听
     * 当滑动之后页面不变的情况下，SCROLL_STATE_IDLE会进行两次回调，中间发生一次SCROLL_STATE_SETTLING
     * scrollState用来判断滚动事件的结束，我们只要最后一次SCROLL_STATE_IDLE
     * @Param: 滚动状态
     */
    fun onScrollStateChanged(newState: Int) {
        when (newState) {
            RecyclerView.SCROLL_STATE_IDLE -> {
                if (scrollState == 1) {
                    //curHolder.startVideo()
                    initMediaPlayer()
                    scrollState = 0
                }

            }
            RecyclerView.SCROLL_STATE_SETTLING -> {
                scrollState++
            }
        }
    }

    /**
     * VideoHolder
     */
    inner class VideoHolder(private val itemBinding: VideoItemBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

        private var curPosition = 0     //存储当前Holder的视频播放位置

        private var isPraised = false

        private var isAnimated = false

        fun getSurfaceViewCreating(): Boolean {
            return itemBinding.video.holder.isCreating
        }

        /**
         * 发出加载MediaPlayer的请求
         */
        fun onBind(position: Int, video: VideoResponse) {
            Log.d(TAG, "viewHolder($position) onBind()")
            this.curPosition = position
            itemBinding.video.holder.addCallback(surfaceCallBack)
            //设置视频信息，使用Glide加载头像
            itemBinding.txtOwnerId.text = video.ownerName
            itemBinding.txtVideoText.text = video.adTxt
            Glide.with(context)
                .load(Repository.getUserImageUrl(video.ownerId))
                .placeholder(R.drawable.place_holder_user)
                .circleCrop()
                .into(itemBinding.imgOwner)
            val clickListener = View.OnClickListener { v ->
                if (isAnimated) {
                    return@OnClickListener
                }
                when (v?.id) {
                    R.id.btn_praise -> {
                        isAnimated = true
                        if (!isPraised) {
                            itemBinding.videoItem.setTransition(R.id.transition_thumb_up)
                            itemBinding.videoItem.transitionToEnd()
                        } else {
                            itemBinding.videoItem.setTransition(R.id.transition_cancel_thumb)
                            itemBinding.videoItem.transitionToEnd()
                        }
                        isPraised = !isPraised
                    }
                    R.id.img_owner -> {
                        Toast.makeText(context, "clicked ${video.ownerName}", Toast.LENGTH_SHORT)
                            .show()
                    }
                    R.id.btn_comment -> {
                        showComments()
                    }
                    R.id.txt_ownerId -> {
                        Toast.makeText(context, "clicked ${video.ownerName}", Toast.LENGTH_SHORT)
                            .show()
                        this@VideoAdapter2.startPlay()
                    }
                    R.id.txt_videoText -> {
                        Toast.makeText(context, "clicked ad", Toast.LENGTH_SHORT).show()
                    }
                    R.id.btn_pause -> {
                        if (mediaPlayer.isPlaying) {
                            mediaPlayer.pause()
                            itemBinding.btnPause.visibility = View.VISIBLE
                        } else {
                            mediaPlayer.start()
                            itemBinding.btnPause.visibility = View.INVISIBLE
                        }
                    }
                    else -> {
                        if (mediaPlayer.isPlaying) {
                            mediaPlayer.pause()
                            itemBinding.btnPause.visibility = View.VISIBLE
                        } else {
                            mediaPlayer.start()
                            itemBinding.btnPause.visibility = View.INVISIBLE
                        }
                    }
                }
            }
            itemBinding.videoItem.setOnClickListener(clickListener)
            itemBinding.imgOwner.setOnClickListener(clickListener)
            itemBinding.btnPraise.setOnClickListener(clickListener)
            itemBinding.btnComment.setOnClickListener(clickListener)
            itemBinding.btnPause.setOnClickListener(clickListener)
            itemBinding.txtOwnerId.setOnClickListener(clickListener)
            itemBinding.txtVideoText.setOnClickListener(clickListener)
            itemBinding.video.setOnClickListener(clickListener)

            //监听MotionLayout的动画状态
            itemBinding.videoItem.setTransitionListener(object : MotionLayout.TransitionListener {
                override fun onTransitionStarted(
                    motionLayout: MotionLayout?,
                    startId: Int,
                    endId: Int
                ) {
                }

                override fun onTransitionChange(
                    motionLayout: MotionLayout?,
                    startId: Int,
                    endId: Int,
                    progress: Float
                ) {
                }

                override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                    isAnimated = false
                }

                override fun onTransitionTrigger(
                    motionLayout: MotionLayout?,
                    triggerId: Int,
                    positive: Boolean,
                    progress: Float
                ) {
                }
            })
        }

        /**
         * 向RecyclerView请求评论区数据
         */
        private fun showComments() {
            this@VideoAdapter2.showComments(videoList[curPosition].id)
        }

        fun getSurfaceHolder(): SurfaceHolder {
            return itemBinding.video.holder
        }

        fun showPauseSign() {
            itemBinding.btnPause.visibility = View.VISIBLE
        }

        fun hidePauseSign() {
            itemBinding.btnPause.visibility = View.INVISIBLE
        }
    }

    class MyPagerSnapHelper : PagerSnapHelper() {}
}

