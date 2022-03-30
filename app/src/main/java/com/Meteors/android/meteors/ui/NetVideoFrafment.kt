package com.Meteors.android.meteors.ui

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.Meteors.android.meteors.MediaPlayerPool
import com.Meteors.android.meteors.R
import com.Meteors.android.meteors.databinding.FragmentMainBinding
import com.Meteors.android.meteors.databinding.VideoItemBinding
import com.Meteors.android.meteors.logic.model.VideoResponse

private const val TAG = "Meteors_NetFragment"

/**
 * NetVideoFragment
 * 播放服务器的视频，对应底部导航栏为推荐
 */
class NetVideoFragment : Fragment() {

    private val viewModel by lazy { ViewModelProvider(this).get(NetFragmentViewModel::class.java) }

    private var _binding: FragmentMainBinding? = null

    private val binding get() = _binding!!      //当前布局的ViewBinding

    private lateinit var mediaPlayerPool: MediaPlayerPool       //MediaPlayer的集中管理工具类

    private lateinit var videoAdapter: VideoAdapter     //Adapter

    @SuppressLint("NotifyDataSetChanged")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        binding.refresh.isRefreshing = true

        //配置RecyclerView
        val pagerSnapHelper = MyPagerSnapHelper()       //PagerSnapHelper用于让RecyclerView只显示一个Item在屏幕上
        videoAdapter = VideoAdapter(viewModel.videos)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = videoAdapter
            pagerSnapHelper.attachToRecyclerView(this)
        }

        viewModel.getVideoList()        //向网络层请求数据，并对数据进行监听
        viewModel.videoList.observe(viewLifecycleOwner, Observer { result ->
            val videos = result.getOrNull()
            if (videos != null) {
                //此处应该设置列表为空时的处理
                viewModel.videos.addAll(videos)
                val windowWidth =
                    requireActivity().windowManager.currentWindowMetrics.bounds.width()     //获取屏幕宽度，用于视频缩放
                mediaPlayerPool = MediaPlayerPool(
                    requireContext(),
                    viewModel.videos,
                    MediaPlayerPool.SOURCE_NET,
                    windowWidth
                )      //实例化mediaPlayerPool
                binding.imgHint.visibility = View.INVISIBLE
                binding.recyclerView.visibility = View.VISIBLE
                videoAdapter.notifyDataSetChanged()
            } else {
                Toast.makeText(requireContext(), "未获取到视频列表", Toast.LENGTH_SHORT).show()
                result.exceptionOrNull()?.printStackTrace()
            }
            binding.refresh.isRefreshing = false
        })
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
            mediaPlayerPool.release()
            binding.refresh.isRefreshing = true
            viewModel.videos.clear()
            viewModel.getVideoList()        //向网络层请求数据，并对数据进行监听
        }
    }

    override fun onPause() {
        super.onPause()
        mediaPlayerPool.pauseVideo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        mediaPlayerPool.release()
    }

    /**
     * VideoHolder
     */
    private inner class VideoHolder(val itemBinding: VideoItemBinding) :
        RecyclerView.ViewHolder(itemBinding.root), View.OnClickListener, SurfaceHolder.Callback {

        private var curPosition = 0     //存储当前Holder的视频播放位置

        private val holder: SurfaceHolder = itemBinding.video.holder    //当前界面的SurfaceHolder

        private var isInitialized = false       //记录Surface的加载状态

        /**
         * 发出加载MediaPlayer的请求
         */
        fun onBind(position: Int, video: VideoResponse) {
            Log.d(TAG, "viewHolder($position) onBind()")
            this.curPosition = position
            mediaPlayerPool.load(position)
            itemBinding.root.setOnClickListener(this)
            holder.addCallback(this)
            //设置文字信息
            itemBinding.txtOwnerId.text = video.ownerId
            itemBinding.txtVideoText.text = video.adTxt
        }

        //按钮点击事件监听
        override fun onClick(v: View?) {
            when (v?.id) {

                R.id.btn_praise -> {
                    Toast.makeText(context, "clicked 点赞", Toast.LENGTH_SHORT).show()
                }
                R.id.btn_comment -> {
                    Toast.makeText(context, "clicked 评论", Toast.LENGTH_SHORT).show()
                }
                R.id.txt_ownerId -> {
                    Toast.makeText(context, "clicked ID", Toast.LENGTH_SHORT).show()
                }
                R.id.btn_pause -> {
                    pauseVideo()
                }
                else -> {
                    pauseVideo()
                }
            }
        }

        /**
         * 由RecyclerViewAdapter调用，用于发出播放请求
         */
        fun startVideo() {
            Log.d(TAG, "Holder-${curPosition} startVideo()")
            mediaPlayerPool.startVideo(curPosition, holder, isInitialized)
            itemBinding.btnPause.visibility = View.INVISIBLE
        }

        /**
         * 暂停或继续播放视频
         */
        fun pauseVideo() {
            if (mediaPlayerPool.isPaused()) {
                mediaPlayerPool.resumeVideo()
                itemBinding.btnPause.visibility = View.INVISIBLE
            } else {
                mediaPlayerPool.pauseVideo()
                itemBinding.btnPause.visibility = View.VISIBLE
            }
        }

        /**
         * 监听surface的加载状态
         */
        override fun surfaceCreated(holder: SurfaceHolder) {
            Log.d(TAG, "surfaceCreated")
            isInitialized = true
            mediaPlayerPool.startVideo(curPosition, holder, isInitialized)
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
     * VideoAdapter
     * RecyclerView适配器，对滚动状态进行监听，在当前屏幕上的VieHolder中进行视频播放
     */
    private inner class VideoAdapter(val videoList: List<VideoResponse>) :
        RecyclerView.Adapter<VideoHolder>() {

        private var curHolder = HashSet<VideoHolder>()      //用于保存当前屏幕上的VideoHolder

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
            curHolder.add(holder)
        }

        /**
         * VideoHolder滑出屏幕
         */
        override fun onViewDetachedFromWindow(holder: VideoHolder) {
            super.onViewDetachedFromWindow(holder)
            curHolder.remove(holder)
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