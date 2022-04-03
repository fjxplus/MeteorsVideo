package com.Meteors.android.meteors.ui.ShortVideo.net

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.Meteors.android.meteors.MediaPlayerPool
import com.Meteors.android.meteors.databinding.FragmentMainBinding
import com.Meteors.android.meteors.ui.NetFragmentViewModel
import com.Meteors.android.meteors.ui.ShortVideo.recyclerView.VideoAdapter
import com.Meteors.android.meteors.ui.ShortVideo.recyclerView.VideoLinearLayoutManager

private const val TAG = "Meteors_NetFragment"

/**
 * @Description: 播放服务器的视频，对应底部导航栏为推荐
 */
class NetVideoFragment : Fragment() {

    private val viewModel by lazy { ViewModelProvider(this).get(NetFragmentViewModel::class.java) }

    private var _binding: FragmentMainBinding? = null

    private val binding get() = _binding!!      //当前布局的ViewBinding

    private lateinit var videoAdapter: VideoAdapter     //Adapter

    private lateinit var recyclerViewLayoutManager: VideoLinearLayoutManager    //LayoutManager

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
        videoAdapter = VideoAdapter(requireContext(), viewModel.videos)
        recyclerViewLayoutManager = VideoLinearLayoutManager(requireContext())
        videoAdapter.setCanScrollVertically = { flag ->
            recyclerViewLayoutManager.setCanScrollVertically(flag)
            binding.refresh.isEnabled = flag
        }
        videoAdapter.getMediaPlayerPool = {
            viewModel.mediaPlayerPool
        }
        videoAdapter.getCommentListResponse = { videoId, setCommentAdapter ->
            //向ViewModel请求评论区数据
        }
        val pagerSnapHelper =
            VideoAdapter.MyPagerSnapHelper()       //PagerSnapHelper用于让RecyclerView只显示一个Item在屏幕上
        binding.recyclerView.apply {
            layoutManager = recyclerViewLayoutManager
            adapter = videoAdapter
            pagerSnapHelper.attachToRecyclerView(this)
        }

        if (!viewModel.mediaPlayerIsInitialiazed()) {
            viewModel.getVideoList()        //向网络层请求数据，并对数据进行监听
        }
        viewModel.videoList.observe(viewLifecycleOwner, Observer { result ->
            val videos = result.getOrNull()
            if (videos != null) {
                //此处应该设置列表为空时的处理
                viewModel.videos.addAll(videos)
                val windowWidth =
                    requireActivity().windowManager.currentWindowMetrics.bounds.width()     //获取屏幕宽度，用于视频缩放
                viewModel.mediaPlayerPool = viewModel.initMediaPlayerPool(
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
            viewModel.refresh()     //触发数据更新
            binding.refresh.isRefreshing = true
            viewModel.getVideoList()        //向网络层请求数据，并对数据进行监听
        }
    }

    override fun onPause() {
        super.onPause()
        videoAdapter.pauseVideo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}