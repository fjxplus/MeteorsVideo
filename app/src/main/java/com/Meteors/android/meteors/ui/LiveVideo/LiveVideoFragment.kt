package com.Meteors.android.meteors.ui.LiveVideo

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.*
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.Meteors.android.meteors.MainApplication
import com.Meteors.android.meteors.R
import com.Meteors.android.meteors.databinding.FragmentLiveBinding
import com.Meteors.android.meteors.logic.model.Comment
import com.Meteors.android.meteors.logic.model.VideoResponse
import com.Meteors.android.meteors.ui.PraiseController
import java.lang.IllegalStateException

private const val TAG = "Meteors_Live_Fragment"
private const val VIDEO_ID = "video_id"
private const val OWNER_ID = "owner_id"
private const val OWNER_NAME = "owner_name"
private const val VIDEO_TEXT = "video_adText"
private const val MESSAGE_UPDATE = 0

/**
 * @Description:  直播界面的Fragment，可以进行礼物的赠送
 */
class LiveVideoFragment : Fragment(), View.OnClickListener {

    /**
     * @Description: 为该Fragment提供构造方法，将视频播放所需要的关键信息附加到argument上
     * @Param: VideoResponse
     * @return:  LiveVideoFragment
     */
    companion object {
        fun newInstance(videoResponse: VideoResponse): LiveVideoFragment {
            val args = Bundle().apply {
                putSerializable(VIDEO_ID, videoResponse.id)
                putSerializable(VIDEO_TEXT, videoResponse.adTxt)
                putSerializable(OWNER_ID, videoResponse.ownerId)
                putSerializable(OWNER_NAME, videoResponse.ownerName)
            }
            return LiveVideoFragment().apply {
                arguments = args
            }
        }
    }

    private val viewModel by lazy { ViewModelProvider(this).get(LiveFragmentViewModel::class.java) }

    private var _binding: FragmentLiveBinding? = null

    private val binding get() = _binding!!

    private val comments get() = viewModel.comments

    private lateinit var video: VideoResponse       //视频信息，由argument中获得

    private lateinit var player: MediaPlayer        //视频播放器

    private lateinit var commentAdapter: LiveCommentAdapter     //公屏的Adapter

    private lateinit var praiseController: PraiseController     //连续点击屏幕点赞的触发器

    private var latestClickTime: Long = 0       //记录上次点击屏幕空闲区域的时间

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)

        video = VideoResponse(
            arguments?.get(VIDEO_ID).toString(),
            arguments?.get(OWNER_ID).toString(),
            arguments?.get(OWNER_NAME).toString(),
            arguments?.get(VIDEO_TEXT).toString()
        )

        //初始化MediaPlayer
        initMediaPlayer()

        //初始化RecyclerView
        commentAdapter = LiveCommentAdapter(viewModel.comments)
        binding.recyclerViewComment.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = commentAdapter
        }

        //监听SurfaceView
        binding.surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            @RequiresApi(Build.VERSION_CODES.R)
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.d(TAG, "surfaceCreated")
                player.setDisplay(holder)
                player.start()
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {}
        })

        praiseController = PraiseController(requireContext(), binding.root)     //实例化点赞控制器

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        //添加事件监听
        binding.btnGift.setOnClickListener(this)
        binding.fragmentLive.setOnClickListener(this)
        //binding.btnGift1.setOnClickListener(this)
        //binding.btnGift2.setOnClickListener(this)
        //binding.btnPack.setOnClickListener(this)
        //binding.root.setTransitionListener(this)
        //监听Switch，更改评论区的可见性
        binding.switchBullet.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                //显示评论区
                binding.recyclerViewComment.visibility = View.VISIBLE
                binding.editComment.visibility = View.VISIBLE
            } else {
                //关闭评论区
                binding.recyclerViewComment.visibility = View.INVISIBLE
                binding.editComment.visibility = View.INVISIBLE
            }
        }
        //监听软键盘的回车发送，更新评论内容至RecyclerView
        binding.editComment.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val editContent = binding.editComment.text.toString()
                if (editContent != "") {
                    synchronized(comments) {
                        viewModel.comments.add(
                            Comment(
                                MainApplication.myId,
                                MainApplication.myName,
                                editContent
                            )
                        )
                        commentAdapter.notifyItemInserted(comments.size - 1)
                        binding.recyclerViewComment.smoothScrollToPosition(comments.size - 1)
                    }
                }
                binding.editComment.text.clear()
            }
            true
        }
        //初始化CommentLoader， 开始加载评论
        initCommentLoader()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initCommentLoader() {
        val updateHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                if (msg.what == MESSAGE_UPDATE) {
                    Log.d("test", "handle main, size = ${comments.size}")
                    viewModel.updateSignal.value = viewModel.updateSignal.value
                }
            }
        }
        viewModel.initCommentLoader(updateHandler)
        viewModel.updateSignal.observe(viewLifecycleOwner, Observer {
            synchronized(comments) {
                if (comments.size > 40) {
                    val temp = comments.drop(20)
                    comments.clear()
                    comments.addAll(temp)
                    commentAdapter.notifyDataSetChanged()
                } else {
                    commentAdapter.notifyItemInserted(comments.size)
                }
                binding.recyclerViewComment.smoothScrollToPosition(comments.size)     //待优化
            }
        })
    }

    override fun onPause() {
        super.onPause()
        player.pause()
    }

    override fun onResume() {
        super.onResume()
        player.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * @Description: 根据Argument中传入的视频信息，加载MediaPlayer，并进行配置
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun initMediaPlayer() {
        if (video.uri.toString() == "") {
            throw IllegalStateException("为传入Fragment视频参数")
        }
        player = MediaPlayer()
        val fd = requireContext().assets.openFd("${video.id}.mp4")
        player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
        player.prepareAsync()
        player.isLooping = true
        player.setOnPreparedListener {
            Log.d(TAG, "player-live prepared()")
            val windowWidth =
                requireActivity().windowManager.currentWindowMetrics.bounds.width()     //获取屏幕宽度，用于视频缩放
            val multiple = windowWidth * 1.0 / player.videoWidth   //根据屏幕宽度获取伸缩系数
            val newHeight = (multiple * player.videoHeight).toInt()
            binding.surfaceView.holder.setFixedSize(windowWidth, newHeight)     //为Surface设置新的宽高
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {

            R.id.btnGift -> {
                //弹出礼物框
            }
            /*
            R.id.btn_gift1 -> {
                binding.root.setTransition(R.id.transition_sendGift1)
                binding.root.transitionToEnd()
            }
            R.id.btn_gift2 -> {
                binding.root.setTransition(R.id.transition_sendGift2)
                binding.root.transitionToEnd()
            }
            R.id.btn_pack -> {
                binding.root.setTransition(R.id.transition_closeGiftTable)
                binding.root.transitionToEnd()
            }
             */
            else -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - latestClickTime <= 400) {
                    Log.d("test", "x = ${binding.btnGift.x}, y = ${binding.btnGift.y}")
                    praiseController.showPraise(binding.btnGift.x, binding.btnGift.y)
                }
                latestClickTime = currentTime
            }
        }
    }

    /**
     * @Description: 重新加载Toolbar
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_live, menu)
    }

    /**
     * @Description: 处理Menu点击事件
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_item_quitThread) {
            if (item.title.toString() == getString(R.string.menu_item_stop_comment_loading)) {
                viewModel.stopLoading()
                item.setTitle(R.string.menu_item_start_comment_loading)
            } else {
                viewModel.startLoading()
                item.setTitle(R.string.menu_item_stop_comment_loading)
            }
        }
        return true
    }

}