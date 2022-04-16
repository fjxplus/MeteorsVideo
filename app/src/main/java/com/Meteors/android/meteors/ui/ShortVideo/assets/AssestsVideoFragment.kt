package com.Meteors.android.meteors.ui.ShortVideo.assets

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.*
import android.widget.PopupWindow
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Meteors.android.meteors.MainActivity
import com.Meteors.android.meteors.MainApplication
import com.Meteors.android.meteors.MediaPlayerPool
import com.Meteors.android.meteors.R
import com.Meteors.android.meteors.databinding.CommentLayoutBinding
import com.Meteors.android.meteors.databinding.FragmentMainBinding
import com.Meteors.android.meteors.logic.model.Comment
import com.Meteors.android.meteors.logic.model.VideoResponse
import com.Meteors.android.meteors.ui.ShortVideo.recyclerView.CommentAdapter
import com.Meteors.android.meteors.ui.ShortVideo.recyclerView.VideoAdapter

private const val TAG = "Meteors_Assets_Fragment"

/**
 * @Description: 播放assets目录下的视频文件，对应底部导航栏为收藏
 */
class AssetsVideoFragment : Fragment(), MainActivity.PlayerController {

    private val viewModel by lazy { ViewModelProvider(this).get(AssetsFragmentViewModel::class.java) }

    private var _binding: FragmentMainBinding? = null

    private lateinit var commentBinding: CommentLayoutBinding       //整个Fragment共用一个评论区

    private val binding get() = _binding!!      //当前布局的ViewBinding

    private lateinit var videoAdapter: VideoAdapter     //Adapter

    private val comments = ArrayList<Comment>()     //用于保存评论区内容

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        commentBinding = CommentLayoutBinding.inflate(layoutInflater, null, false)
        commentBinding.recyclerViewComment.layoutManager = LinearLayoutManager(requireContext())

        //实例化MediaPlayerPool
        val videoList = getVideoList()

        if (!viewModel.mediaPlayerIsInitialiazed()) {
            val windowWidth =
                requireActivity().windowManager.currentWindowMetrics.bounds.width()     //获取屏幕宽度，用于视频缩放
            viewModel.mediaPlayerPool =
                viewModel.initMediaPlayerPool(
                    requireContext(),
                    videoList,
                    MediaPlayerPool.SOURCE_ASSETS,
                    windowWidth
                )
        }

        //初始化RecyclerView
        initVideoAdapter(videoList)

        //监听评论数据的返回
        viewModel.comments.observe(viewLifecycleOwner, Observer { result ->
            val commentListResponse = result.getOrNull()
            if (commentListResponse != null) {
                comments.clear()
                comments.addAll(commentListResponse.comments)
                showComment()
            } else {
                Toast.makeText(requireContext(), "网络状态不佳", Toast.LENGTH_SHORT).show()
                result.exceptionOrNull()?.printStackTrace()
            }
        })

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

        //处理评论事件
        commentBinding.btnCommentCommit.setOnClickListener {
            val content = commentBinding.editComment.text.toString()
            if (content != "") {
                val comment = Comment(MainApplication.myId,MainApplication.myName, content)
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
        if (!viewModel.mediaPlayerPool.isPaused()) {
            videoAdapter.pauseVideo()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * @Description: Assets目录下的视频信息
     */
    private fun getVideoList(): List<VideoResponse> {
        return listOf(
            VideoResponse("VID_1", "user5", "@吕子乔", "我早就说过：人生苦短，及时行乐才是王道。"),
            VideoResponse("VID_2", "user6", "@吕子乔", "我早就说过：人生苦短，及时行乐才是王道。"),
            VideoResponse("VID_3", "user7", "@吕子乔", "我早就说过：人生苦短，及时行乐才是王道。"),
            VideoResponse("VID_4", "user8", "@吕子乔", "我早就说过：人生苦短，及时行乐才是王道。")
        )
    }

    /**
     * @Description: 对RecyclerView和VideoAdapter进行配置，初始化lateinit方法
     * @Param: videoList视频列表
     * @return: unit
     */
    private fun initVideoAdapter(videoList: List<VideoResponse>) {
        //配置RecyclerView
        videoAdapter = VideoAdapter(requireContext(), videoList)
        val pagerSnapHelper =
            VideoAdapter.MyPagerSnapHelper()     //PagerSnapHelper用于让RecyclerView只显示一个Item在屏幕上

        //提供拿到MediaPlayer的方法
        videoAdapter.getMediaPlayerPool = {
            viewModel.mediaPlayerPool
        }
        //获取评论区的接口方法
        videoAdapter.showComments = { videoId ->
            viewModel.getComments(videoId)
        }
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

        val popUpView = PopupWindow(
            commentBinding.root,
            ViewGroup.LayoutParams.MATCH_PARENT,
            (binding.root.height * 0.7).toInt(),
            true
        )
        popUpView.isOutsideTouchable = true
        popUpView.isFocusable = true
        popUpView.animationStyle = R.style.anim_comment
        popUpView.showAtLocation(binding.root, Gravity.BOTTOM, 0, 0)    //在底部展示PopUpView
    }
}