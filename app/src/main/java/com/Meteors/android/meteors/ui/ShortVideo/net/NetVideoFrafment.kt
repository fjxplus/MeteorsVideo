package com.Meteors.android.meteors.ui.ShortVideo.net

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Meteors.android.meteors.*
import com.Meteors.android.meteors.databinding.CommentLayoutBinding
import com.Meteors.android.meteors.databinding.FragmentMainBinding
import com.Meteors.android.meteors.logic.model.Comment
import com.Meteors.android.meteors.ui.NetFragmentViewModel
import com.Meteors.android.meteors.ui.ShortVideo.recyclerView.CommentAdapter
import com.Meteors.android.meteors.ui.ShortVideo.recyclerView.VideoAdapter2

private const val TAG = "Meteors_NetFragment"

/**
 * @Description: 播放服务器的视频，对应底部导航栏为推荐
 */
class NetVideoFragment : Fragment(), MainActivity.PlayerController {

    private val viewModel by lazy { ViewModelProvider(this).get(NetFragmentViewModel::class.java) }

    private var _binding: FragmentMainBinding? = null

    private lateinit var commentBinding: CommentLayoutBinding       //整个Fragment共用一个评论区

    private val binding get() = _binding!!      //当前布局的ViewBinding

    private lateinit var videoAdapter: VideoAdapter2     //Adapter

    private val comments = ArrayList<Comment>()         //保存评论区内容

    private val popupWindow by lazy {       //展示评论区的PopupWindow
        initPopupWindow()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        binding.refresh.isRefreshing = true
        commentBinding = CommentLayoutBinding.inflate(layoutInflater, null, false)
        commentBinding.recyclerViewComment.layoutManager = LinearLayoutManager(requireContext())
        //配置RecyclerView
        initVideoAdapter()
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //监听视频列表
        viewModel.videoList.observe(viewLifecycleOwner, Observer { result ->
            val videos = result.getOrNull()
            if (videos != null) {
                //此处应该设置列表为空时的处理
                viewModel.videos.addAll(videos)
                binding.imgHint.visibility = View.INVISIBLE
                binding.recyclerView.visibility = View.VISIBLE
                videoAdapter.notifyDataSetChanged()
            } else {
                Toast.makeText(requireContext(), "未获取到视频列表", Toast.LENGTH_SHORT).show()
                result.exceptionOrNull()?.printStackTrace()
            }
            binding.refresh.isRefreshing = false
        })

        //监听评论列表信息
        viewModel.comments.observe(viewLifecycleOwner, Observer { result ->
            val commentListResponse = result.getOrNull()
            if (commentListResponse != null) {
                comments.clear()
                comments.addAll(commentListResponse.comments)
                showComment()       //展示评论区
            } else {
                Toast.makeText(requireContext(), "网络状态不佳", Toast.LENGTH_SHORT).show()
                result.exceptionOrNull()?.printStackTrace()
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
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
            /*
            viewModel.refresh()     //触发数据更新
            binding.refresh.isRefreshing = true
            viewModel.getVideoList()        //向网络层请求数据，并对数据进行监听

             */
        }

        //为评论按钮设置监听
        commentBinding.btnCommentCommit.setOnClickListener { view ->
            val content = commentBinding.editComment.text.toString()
            if (content != "") {
                val comment = Comment(MainApplication.myId, MainApplication.myName, content)
                comments.add(0, comment)
                commentBinding.recyclerViewComment.apply {
                    adapter?.notifyItemInserted(0)
                    smoothScrollToPosition(0)
                }
                commentBinding.editComment.text?.clear()
                Toast.makeText(requireContext(), "评论成功！", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun startWorking() {
        videoAdapter.startWork()
    }

    override fun stopWorking() {
        videoAdapter.stopWork()
    }

    override fun onPause() {
        super.onPause()
        /*
        if (!viewModel.mediaPlayerPool.isPaused()) {
            videoAdapter.pauseVideo()
        }

         */
        videoAdapter.pauseVideo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * @Description: 对RecyclerView和VideoAdapter进行配置，初始化lateinit方法
     * @Param: videoList视频列表
     * @return: unit
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun initVideoAdapter() {
        val mediaResourceManager = MediaSourceManager(this)
        val windowWidth = activity?.windowManager?.currentWindowMetrics?.bounds?.width()
        videoAdapter = VideoAdapter2(
            requireContext(),
            viewModel.videos,
            mediaResourceManager,
            windowWidth!!
        ) { videoId ->
            //向ViewModel请求评论区数据
            viewModel.getComments(videoId)
        }

        val pagerSnapHelper =
            VideoAdapter2.MyPagerSnapHelper()       //PagerSnapHelper用于让RecyclerView只显示一个Item在屏幕上
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = videoAdapter
            pagerSnapHelper.attachToRecyclerView(this)
        }
    }

    /**
     * @Description: 使用PopUpView进行评论区的展示
     * @Param: commentsViewModel返回的评论信息
     * @return: unit
     */
    private fun showComment() {
        commentBinding.recyclerViewComment.adapter = CommentAdapter(requireContext(), comments)
        commentBinding.editComment.text?.clear()
        popupWindow.showAtLocation(binding.root, Gravity.BOTTOM, 0, 0)    //在底部展示PopUpView
    }

    private fun initPopupWindow(): PopupWindow {
        val popUpWindow = PopupWindow(
            commentBinding.root,
            ViewGroup.LayoutParams.MATCH_PARENT,
            (binding.root.height * 0.7).toInt(),
            true
        )
        popUpWindow.isOutsideTouchable = true
        popUpWindow.isFocusable = true
        popUpWindow.animationStyle = R.style.anim_comment
        return popUpWindow
    }
}