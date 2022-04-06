package com.Meteors.android.meteors.ui.LiveVideo

import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.annotation.RequiresApi
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.Meteors.android.meteors.MainApplication
import com.Meteors.android.meteors.R
import com.Meteors.android.meteors.databinding.FragmentLiveBinding
import com.Meteors.android.meteors.logic.model.Comment
import com.Meteors.android.meteors.logic.model.VideoResponse

private const val TAG = "Meteors_Live_Fragment"
private const val VIDEO_ID = "video_id"
private const val OWNER_ID = "owner_id"
private const val OWNER_NAME = "owner_name"
private const val VIDEO_TEXT = "video_adText"

/**
 * @Description:  直播界面的Fragment，可以进行礼物的赠送
 */
class LiveVideoFragment : Fragment(), View.OnClickListener, MotionLayout.TransitionListener {

    /**
     * @Description: 为该Fragment提供构造方法，传入视频播放所需要的关键信息
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

    private var _binding: FragmentLiveBinding? = null

    private val binding get() = _binding!!

    private lateinit var video: VideoResponse

    private lateinit var player: MediaPlayer

    private val comments = ArrayList<Comment>()

    private lateinit var commentAdapter: LiveCommentAdapter

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveBinding.inflate(inflater, container, false)

        video = VideoResponse(
            arguments?.get(VIDEO_ID).toString(),
            arguments?.get(OWNER_ID).toString(),
            arguments?.get(OWNER_NAME).toString(),
            arguments?.get(VIDEO_TEXT).toString()
        )

        initMediaPlayer()       //初始化MediaPlayer

        comments.add(Comment("user_3", "马牛逼", "马牛逼祝您666"))
        comments.add(Comment("user_default", "范嘉行范佳兴范佳兴范佳兴范佳兴", "牛啊牛啊"))
        comments.add(Comment("user_2", "范嘉行范佳兴范佳兴范佳兴范佳兴", "好啊好啊好啊好啊好啊好啊好啊好啊好啊好啊好啊好啊好啊好啊好啊"))

        commentAdapter = LiveCommentAdapter(comments)
        binding.recyclerViewComment.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = commentAdapter
        }

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
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        binding.btnGift.setOnClickListener(this)
        binding.btnGift1.setOnClickListener(this)
        binding.btnGift2.setOnClickListener(this)
        binding.btnPack.setOnClickListener(this)
        binding.fragmentLive.setOnClickListener(this)
        binding.root.setTransitionListener(this)
        binding.switchBullet.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                Log.d("test", "bullet switch on")
                //显示评论区
                binding.recyclerViewComment.visibility = View.VISIBLE
                binding.editComment.visibility = View.VISIBLE
            } else {
                Log.d("test", "bullet switch off")
                //关闭评论区
                binding.recyclerViewComment.visibility = View.INVISIBLE
                binding.editComment.visibility = View.INVISIBLE
            }
        }
        binding.editComment.setOnEditorActionListener { v, actionId, event ->
            if(actionId == EditorInfo.IME_ACTION_SEND){
                val editContent = binding.editComment.text.toString()
                if(editContent != ""){
                    synchronized(comments){
                        comments.add(Comment(MainApplication.myId, MainApplication.myName, editContent))
                        commentAdapter.notifyItemInserted(comments.size - 1)
                        binding.recyclerViewComment.smoothScrollToPosition(comments.size - 1)
                    }
                }
                binding.editComment.text.clear()
                true
            }
            false
        }
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

    @RequiresApi(Build.VERSION_CODES.R)
    private fun initMediaPlayer() {
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
                Log.d("test", "click btnFGift")
                binding.root.setTransition(R.id.transition_openGiftTable)
                binding.root.transitionToEnd()
            }
            R.id.btn_gift1 -> {
                Log.d("test", "click btnFGift1")
                binding.root.setTransition(R.id.transition_sendGift1)
                binding.root.transitionToEnd()
            }
            R.id.btn_gift2 -> {
                Log.d("test", "click btnFGift2")
                binding.root.setTransition(R.id.transition_sendGift2)
                binding.root.transitionToEnd()
            }
            R.id.btn_pack -> {
                Log.d("test", "click btnFPack")
                binding.root.setTransition(R.id.transition_closeGiftTable)
                binding.root.transitionToEnd()
            }
            else -> {
                Log.d("test", "click otherWhere")
            }
        }
    }

    override fun onTransitionStarted(motionLayout: MotionLayout?, startId: Int, endId: Int) {

    }

    override fun onTransitionChange(
        motionLayout: MotionLayout?,
        startId: Int,
        endId: Int,
        progress: Float
    ) {

    }

    override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {

    }

    override fun onTransitionTrigger(
        motionLayout: MotionLayout?,
        triggerId: Int,
        positive: Boolean,
        progress: Float
    ) {

    }

}