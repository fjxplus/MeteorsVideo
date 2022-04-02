package com.Meteors.android.meteors.ui

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.Meteors.android.meteors.MediaPlayerPool
import com.Meteors.android.meteors.R
import com.Meteors.android.meteors.databinding.FragmentMainBinding
import com.Meteors.android.meteors.databinding.VideoItemBinding
import com.Meteors.android.meteors.logic.model.VideoResponse

private const val TAG = "Meteors_Assets_Fragment"

/**
* @Description: 播放assets目录下的视频文件，对应底部导航栏为收藏
*/
class AssetsVideoFragment : Fragment() {

    private val viewModel by lazy { ViewModelProvider(this).get(AssetsFragmentViewModel::class.java) }

    private var _binding: FragmentMainBinding? = null

    private val binding get() = _binding!!      //当前布局的ViewBinding

    private lateinit var videoAdapter: VideoAdapter     //Adapter

    private lateinit var recyclerViewLayoutManager: CommentLinearLayoutManager

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)

        //实例化MediaPlayerPool
        val videoList = getVideoList()

        if(!viewModel.mediaPlayerIsInitialiazed()){
            val windowWidth =
                requireActivity().windowManager.currentWindowMetrics.bounds.width()     //获取屏幕宽度，用于视频缩放
            viewModel.mediaPlayerPool =
                viewModel.initMediaPlayerPool(requireContext(), videoList, MediaPlayerPool.SOURCE_ASSETS, windowWidth)
        }

        //配置RecyclerView
        val pagerSnapHelper = MyPagerSnapHelper()       //PagerSnapHelper用于让RecyclerView只显示一个Item在屏幕上
        recyclerViewLayoutManager = CommentLinearLayoutManager(requireContext())
        videoAdapter = VideoAdapter(videoList)
        binding.recyclerView.apply {
            layoutManager = recyclerViewLayoutManager
            adapter = videoAdapter
            pagerSnapHelper.attachToRecyclerView(this)
        }
        binding.imgHint.visibility = View.INVISIBLE
        binding.recyclerView.visibility = View.VISIBLE
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        //监听滑动状态
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                videoAdapter.onScrollStateChanged(newState)
            }
        })

        //刷新组件的监听
        binding.refresh.setOnRefreshListener {
            Log.d(TAG, "refresh")
            var sum = 0.0
            for (i in 0 until 10000) {
                sum += i
            }
            binding.refresh.isRefreshing = false
        }
    }

    override fun onPause() {
        super.onPause()
        videoAdapter.curHolder.pauseVideo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getVideoList(): List<VideoResponse>{
        return listOf(
            VideoResponse("VID_1", "@吕子乔", "我早就说过：人生苦短，及时行乐才是王道。"),
            VideoResponse("VID_2", "@吕子乔", "我早就说过：人生苦短，及时行乐才是王道。"),
            VideoResponse("VID_3", "@吕子乔", "我早就说过：人生苦短，及时行乐才是王道。"),
            VideoResponse("VID_4", "@吕子乔", "我早就说过：人生苦短，及时行乐才是王道。")
        )
    }

    /**
     * VideoHolder
     */
    private inner class VideoHolder(val itemBinding: VideoItemBinding) :
        RecyclerView.ViewHolder(itemBinding.root), SurfaceHolder.Callback {

        private var curPosition = 0     //存储当前Holder的视频播放位置

        private val holder: SurfaceHolder = itemBinding.video.holder    //当前界面的SurfaceHolder

        private var isInitialized = false       //记录Surface的加载状态
        private var isCommentOpen = false
        private var isPraised = false
        private var isAnimated = false
        /**
         * 发出加载MediaPlayer的请求
         */
        fun onBind(position: Int, video: VideoResponse) {
            Log.d(TAG, "viewHolder($position) onBind()")
            this.curPosition = position
            viewModel.mediaPlayerPool.load(position)
            holder.addCallback(this)
            val data = listOf<String>("fa", "fdgfa", "fasdwer", "trer", "fa", "fdgfa", "fasdwer", "trer", "fa", "fdgfa", "fasdwer", "trer", "fa", "fdgfa", "fasdwer", "trer")
            val listViewAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, data)
            itemBinding.listView.adapter = listViewAdapter
            //设置文字信息
            itemBinding.txtOwnerId.text = video.ownerId
            itemBinding.txtVideoText.text = video.adTxt
            val clickListener = View.OnClickListener { v ->
                if(isAnimated){
                    return@OnClickListener
                }
                if (isCommentOpen){     //如果当前评论区已打开，先关闭评论区
                    isAnimated = true
                    itemBinding.videoItem.setTransition(R.id.transition_commentOpen_origin)
                    itemBinding.videoItem.transitionToEnd()
                    return@OnClickListener
                }
                when (v?.id) {
                    R.id.btn_praise -> {
                        Log.d("test", "isPraised = $isPraised")
                        isAnimated = true
                        if(!isPraised){
                            itemBinding.videoItem.setTransition(R.id.transition_thumb_up)
                            itemBinding.videoItem.transitionToEnd()
                        }else{
                            itemBinding.videoItem.setTransition(R.id.transition_cancel_thumb)
                            itemBinding.videoItem.transitionToEnd()
                        }
                        isPraised = !isPraised
                    }
                    R.id.btn_comment -> {
                        Log.d("test", "comment")
                        isAnimated = true
                        itemBinding.videoItem.setTransition(R.id.transition_origin_commentOpen)
                        itemBinding.videoItem.transitionToEnd()
                        isCommentOpen = true
                    }
                    R.id.txt_ownerId -> {
                        Toast.makeText(context, "clicked ID", Toast.LENGTH_SHORT).show()
                    }
                    R.id.txt_videoText -> {
                        Toast.makeText(context, "clicked ad", Toast.LENGTH_SHORT).show()
                    }
                    R.id.btn_pause -> {
                        pauseVideo()
                    }
                    else -> {
                        pauseVideo()
                    }
                }
            }
            itemBinding.videoItem.setOnClickListener(clickListener)
            itemBinding.btnPraise.setOnClickListener(clickListener)
            itemBinding.btnComment.setOnClickListener(clickListener)
            itemBinding.btnPause.setOnClickListener(clickListener)
            itemBinding.txtOwnerId.setOnClickListener(clickListener)
            itemBinding.txtVideoText.setOnClickListener(clickListener)
            itemBinding.video.setOnClickListener(clickListener)

            //监听MotionLayout的动画状态
            itemBinding.videoItem.setTransitionListener(object: MotionLayout.TransitionListener{
                override fun onTransitionStarted(
                    motionLayout: MotionLayout?,
                    startId: Int,
                    endId: Int
                ) {
                    if(endId == R.id.constrainSet_comment_open){        //评论区打开时RecyclerView不应滑动
                        recyclerViewLayoutManager.setCanScrollVertically(false)
                        binding.refresh.isEnabled = false
                        isCommentOpen = true
                    }else if(endId == R.id.constrainSet_origin){
                        isCommentOpen = false
                    }
                }

                override fun onTransitionChange(
                    motionLayout: MotionLayout?,
                    startId: Int,
                    endId: Int,
                    progress: Float
                ) { }
                override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                    if(currentId == R.id.constrainSet_origin){         //评论区关闭时，RecyclerView可以滑动
                        recyclerViewLayoutManager.setCanScrollVertically(true)
                        binding.refresh.isEnabled = true
                    }
                    isAnimated = false
                }
                override fun onTransitionTrigger(
                    motionLayout: MotionLayout?,
                    triggerId: Int,
                    positive: Boolean,
                    progress: Float
                ) { }
            })
        }

        /**
         * 由RecyclerViewAdapter调用，用于发出播放请求
         */
        fun startVideo() {
            Log.d(TAG, "Holder-${curPosition} startVideo()")
            viewModel.mediaPlayerPool.startVideo(curPosition, holder, isInitialized)
            itemBinding.btnPause.visibility = View.INVISIBLE
        }

        /**
         * 暂停或继续播放视频
         */
        fun pauseVideo(){
            if (viewModel.mediaPlayerPool.isPaused()) {
                viewModel.mediaPlayerPool.resumeVideo()
                itemBinding.btnPause.visibility = View.INVISIBLE
            } else {
                viewModel.mediaPlayerPool.pauseVideo()
                itemBinding.btnPause.visibility = View.VISIBLE
            }
        }

        /**
         * 监听surface的加载状态
         */
        override fun surfaceCreated(holder: SurfaceHolder) {
            Log.d(TAG, "surfaceCreated")
            isInitialized = true
            viewModel.mediaPlayerPool.startVideo(curPosition, holder, isInitialized)
            itemBinding.btnPause.visibility = View.INVISIBLE
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

    /**
    * @Description: RecyclerView适配器，对滚动状态进行监听，在当前屏幕上的VieHolder中进行视频播放
    */
    private inner class VideoAdapter(val videoList: List<VideoResponse>) :
        RecyclerView.Adapter<VideoHolder>() {

        var curHolderContainer = HashSet<VideoHolder>()      //用于保存当前屏幕上的VideoHolder
        val curHolder: VideoHolder get() = curHolderContainer.first()
        private var scrollState = 0     //用于判断滚动状态

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoHolder {
            val itemBinding = VideoItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VideoHolder(itemBinding)
        }

        @RequiresApi(Build.VERSION_CODES.N)
        override fun onBindViewHolder(holder: VideoHolder, position: Int) {
            val video = videoList[position]
            holder.onBind(position, video)
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
    }

    private class MyPagerSnapHelper : PagerSnapHelper() {}
}