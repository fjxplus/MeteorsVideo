package com.Meteors.android.meteors.ui.ShortVideo.assets

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.Meteors.android.meteors.MediaPlayerPool
import com.Meteors.android.meteors.databinding.FragmentMainBinding
import com.Meteors.android.meteors.logic.model.VideoResponse
import com.Meteors.android.meteors.ui.ShortVideo.recyclerView.VideoAdapter
import com.Meteors.android.meteors.ui.ShortVideo.recyclerView.VideoLinearLayoutManager

private const val TAG = "Meteors_Assets_Fragment"

/**
 * @Description: 播放assets目录下的视频文件，对应底部导航栏为收藏
 */
class AssetsVideoFragment : Fragment() {

    private val viewModel by lazy { ViewModelProvider(this).get(AssetsFragmentViewModel::class.java) }

    private var _binding: FragmentMainBinding? = null

    private val binding get() = _binding!!      //当前布局的ViewBinding

    private lateinit var videoAdapter: VideoAdapter     //Adapter

    private lateinit var recyclerViewLayoutManager: VideoLinearLayoutManager

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)

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

        //配置RecyclerView
        videoAdapter = VideoAdapter(requireContext(), videoList)
        recyclerViewLayoutManager = VideoLinearLayoutManager(requireContext())
        val pagerSnapHelper =
            VideoAdapter.MyPagerSnapHelper()     //PagerSnapHelper用于让RecyclerView只显示一个Item在屏幕上
        //实现VideoAdapter中未实现的高阶函数，需要Fragment提供实现，可以使用接口代替
        videoAdapter.setCanScrollVertically = { flag ->
            recyclerViewLayoutManager.setCanScrollVertically(flag)
            binding.refresh.isEnabled = flag
        }
        //提供拿到MediaPlayer的方法
        videoAdapter.getMediaPlayerPool = {
            viewModel.mediaPlayerPool
        }
        //获取评论区的接口方法
        videoAdapter.getCommentListResponse = {videoId, setCommentAdapter ->
            viewModel.getComments(videoId)
            viewModel.comments.observe(viewLifecycleOwner, Observer { result ->
                val commentListResponse = result.getOrNull()
                if(commentListResponse != null){
                    if(commentListResponse.videoId == videoId){
                        setCommentAdapter(commentListResponse.comments)
                    }
                }else{
                    result.exceptionOrNull()?.printStackTrace()
                }
            })
        }
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
        if(!viewModel.mediaPlayerPool.isPaused()){
            videoAdapter.pauseVideo()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getVideoList(): List<VideoResponse> {
        return listOf(
            VideoResponse("VID_1", "user5", "@吕子乔", "我早就说过：人生苦短，及时行乐才是王道。"),
            VideoResponse("VID_2", "user6", "@吕子乔", "我早就说过：人生苦短，及时行乐才是王道。"),
            VideoResponse("VID_3", "user7", "@吕子乔", "我早就说过：人生苦短，及时行乐才是王道。"),
            VideoResponse("VID_4", "user8", "@吕子乔", "我早就说过：人生苦短，及时行乐才是王道。")
        )
    }
}