package com.Meteors.android.meteors.ui.ShortVideo.recyclerView

import android.content.Context
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
import com.Meteors.android.meteors.R
import com.Meteors.android.meteors.databinding.VideoItemBinding
import com.Meteors.android.meteors.logic.model.VideoResponse
import com.Meteors.android.meteors.logic.network.Repository
import com.bumptech.glide.Glide

private const val TAG = "Meteors_VideoAdapter"

/**
 * @Description: RecyclerView适配器，对滚动状态进行监听，在当前屏幕上的VieHolder中进行视频播放
 */
class VideoAdapter(
    val context: Context,
    private val videoList: List<VideoResponse>
) :
    RecyclerView.Adapter<VideoAdapter.VideoHolder>() {

    private var curHolderContainer = HashSet<VideoHolder>()      //用于保存当前屏幕上的VideoHolder

    private val curHolder: VideoHolder get() = curHolderContainer.first()

    private var scrollState = 0     //用于判断滚动状态

    lateinit var getMediaPlayerPool: () -> MediaPlayerPool      //向Fragment获取MediaPlayerPool

    lateinit var showComments: (videoId: String) -> Unit        //Fragment展示评论区

    val mediaPlayerPool: MediaPlayerPool get() = getMediaPlayerPool()

    private var isWorking = false       //当前是否处于可交互状态，是否处于前台

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
                    curHolder.startVideo()
                    scrollState = 0
                }
            }
            RecyclerView.SCROLL_STATE_SETTLING -> {
                scrollState++
            }
        }
    }

    /**
    * @Description: 当前Fragment正在交互， isWorking为true，可以播放视频
    */
    fun startWork(){
        isWorking = true
        curHolder.startVideo()
    }

    /**
    * @Description: 当前Fragment停止交互，暂停播放视频, isWorking设置为false
    */
    fun stopWork(){
        pauseVideo()
        isWorking = false
    }

    /**
    * @Description: 暂停当前界面视频的播放
    */
    fun pauseVideo() {
        curHolder.pauseVideo()
    }

    /**
     * VideoHolder
     */
    inner class VideoHolder(private val itemBinding: VideoItemBinding) :
        RecyclerView.ViewHolder(itemBinding.root), SurfaceHolder.Callback {

        private var curPosition = 0     //存储当前Holder的视频播放位置

        private val holder: SurfaceHolder = itemBinding.video.holder    //当前界面的SurfaceHolder

        private var isInitialized = false       //记录Surface的加载状态

        private var isPraised = false

        private var isAnimated = false

        /**
         * 发出加载MediaPlayer的请求
         */
        fun onBind(position: Int, video: VideoResponse) {
            Log.d(TAG, "viewHolder($position) onBind()")
            this.curPosition = position
            mediaPlayerPool.load(position)
            holder.addCallback(this)
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
                        Toast.makeText(context, "clicked ${video.ownerName}", Toast.LENGTH_SHORT).show()
                    }
                    R.id.btn_comment -> {
                        showComments()
                    }
                    R.id.txt_ownerId -> {
                        Toast.makeText(context, "clicked ${video.ownerName}", Toast.LENGTH_SHORT).show()
                    }
                    R.id.txt_videoText -> {
                        Toast.makeText(context, "clicked ad", Toast.LENGTH_SHORT).show()
                    }
                    R.id.btn_pause -> {
                        if (!mediaPlayerPool.isPaused()) {
                            mediaPlayerPool.pauseVideo()
                            itemBinding.btnPause.visibility = View.VISIBLE
                        }else{
                            mediaPlayerPool.resumeVideo()
                            itemBinding.btnPause.visibility = View.INVISIBLE
                        }
                    }
                    else -> {
                        if (!mediaPlayerPool.isPaused()) {
                            mediaPlayerPool.pauseVideo()
                            itemBinding.btnPause.visibility = View.VISIBLE
                        }else{
                            mediaPlayerPool.resumeVideo()
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
         * 由RecyclerViewAdapter调用，用于发出播放请求
         */
        fun startVideo() {
            Log.d(TAG, "Holder-${curPosition} startVideo()")
            if (isWorking){
                mediaPlayerPool.startVideo(curPosition, holder, isInitialized)
                itemBinding.btnPause.visibility = View.INVISIBLE
            }
        }

        /**
         * 暂停或继续播放视频
         */
        fun pauseVideo() {
            if (!mediaPlayerPool.isPaused()) {
                mediaPlayerPool.pauseVideo()
                itemBinding.btnPause.visibility = View.VISIBLE
            }
        }

        fun resumeVideo(){
            if (mediaPlayerPool.isPaused()) {
                mediaPlayerPool.pauseVideo()
                itemBinding.btnPause.visibility = View.INVISIBLE
            }
        }

        /**
         * 监听surface的加载状态
         */
        override fun surfaceCreated(holder: SurfaceHolder) {
            Log.d(TAG, "surfaceCreated")
            isInitialized = true
            if(isWorking){
                mediaPlayerPool.startVideo(curPosition, holder, isInitialized)
                itemBinding.btnPause.visibility = View.INVISIBLE
            }
        }

        override fun surfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {}

        /**
         * 向RecyclerView请求评论区数据
         */
        private fun showComments() {
            this@VideoAdapter.showComments(videoList[curPosition].id)
        }
    }

    class MyPagerSnapHelper : PagerSnapHelper() {}
}

