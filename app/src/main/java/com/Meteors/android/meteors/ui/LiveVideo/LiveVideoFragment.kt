package com.Meteors.android.meteors.ui.LiveVideo

import android.annotation.SuppressLint
import android.app.Dialog
import android.media.MediaPlayer
import android.os.*
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.Meteors.android.meteors.MainActivity
import com.Meteors.android.meteors.MainApplication
import com.Meteors.android.meteors.R
import com.Meteors.android.meteors.databinding.DialogGiftLayoutBinding
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
class LiveVideoFragment : Fragment(), View.OnClickListener, MainActivity.PlayerController {

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

    private var width: Int = 1080       //屏幕宽度

    private var height: Int = 1920      //屏幕高度

    private val comments get() = viewModel.comments

    private lateinit var video: VideoResponse       //视频信息，由argument中获得

    private lateinit var player: MediaPlayer        //视频播放器

    private lateinit var commentAdapter: LiveCommentAdapter     //公屏的Adapter

    private lateinit var praiseController: PraiseController     //连续点击屏幕点赞的触发器

    private var latestClickTime: Long = 0       //记录上次点击屏幕空闲区域的时间

    private lateinit var dialogBinding: DialogGiftLayoutBinding         //礼物中心的布局

    private lateinit var giftDialog: Dialog         //礼物中心的Dialog

    private var curSelectGift = 0       //默认当前选取的礼物为第1个

    private lateinit var giftViewList: List<ImageButton>        //保存礼物按钮的数组

    private var isWorking = false

    //管理礼物的公屏展示
    private val giftManager: GiftManager by lazy {
        val endY = binding.containerComment.y.toInt()
        val startY = binding.containerComment.y.toInt() - 3 * 250
        GiftManager(requireContext(), binding.root, 3, width, startY, endY).apply {
            setCountDownTick = { time ->
                dialogBinding.btnSendGift.text = "$time"        //回调更新按钮倒计时
            }
            endSendCallback = {
                giftDialog.dismiss()        //隐藏Dialog
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)

        //获取屏幕参数
        width = activity?.windowManager?.currentWindowMetrics?.bounds?.width() ?: width
        height = activity?.windowManager?.currentWindowMetrics?.bounds?.height() ?: height

        //从argument中取出视频信息
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
                startPlaying()
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

        initGiftDialog()        //实例化dialogBinding和GiftDialog，构建礼物中心

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        //添加事件监听
        binding.btnGift.setOnClickListener(this)
        binding.fragmentLive.setOnClickListener(this)
        //礼物中心的事件监听
        dialogBinding.btnSendGift.setOnClickListener(this)
        dialogBinding.radioSendOnce.isChecked = true
        for(view in giftViewList){
            view.setOnClickListener(this)
        }
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
                    viewModel.updateSignal.value = viewModel.updateSignal.value
                }
            }
        }
        viewModel.initCommentLoader(updateHandler)      //初始化评论加载器
        viewModel.updateSignal.observe(viewLifecycleOwner, Observer {
            synchronized(comments) {
                if (comments.size > 40) {       //评论数大于40，进行数组瘦身
                    val temp = comments.drop(20)
                    comments.clear()
                    comments.addAll(temp)
                    commentAdapter.notifyDataSetChanged()       //更新视图
                } else {
                    commentAdapter.notifyItemInserted(comments.size)
                }
                binding.recyclerViewComment.smoothScrollToPosition(comments.size)     //滑动
            }
        })
    }

    /**
     * @Description: 构建礼物中心的Dialog
     */
    private fun initGiftDialog() {
        dialogBinding = DialogGiftLayoutBinding.inflate(layoutInflater)
        giftViewList = listOf(
            dialogBinding.btnGift1,
            dialogBinding.btnGift2,
            dialogBinding.btnGift3,
            dialogBinding.btnGift4,
            dialogBinding.btnGift5,
            dialogBinding.btnGift6,
            dialogBinding.btnGift7,
            dialogBinding.btnGift8,
        )
        giftDialog = Dialog(requireContext())
        giftDialog.setTitle("礼物中心")
        giftDialog.setContentView(dialogBinding.root)
        val dialogWindow = giftDialog.window
        val layoutParams = dialogWindow?.attributes?.apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            gravity = Gravity.BOTTOM
        }
        dialogWindow?.setDimAmount(0f)      //取消背景变灰
        dialogWindow?.attributes = layoutParams
        giftDialog.setOnDismissListener {
            setGiftSelectState(0)       //重置为默认选中第一个礼物
            dialogBinding.radioSendOnce.isChecked = true        //重置为礼物单击模式
            dialogBinding.btnSendGift.setText(R.string.btn_send)        //重置按钮text为“发送”
        }
        dialogBinding.btnGift1.setBackgroundResource(android.R.color.darker_gray)       //默认选中第一个礼物
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

            //右下角礼物中心按钮
            R.id.btnGift -> {
                giftDialog.show()       //展开礼物中心
            }
            //发送礼物按钮
            R.id.btn_sendGift -> {
                if (dialogBinding.radioSendOnce.isChecked) {     //单击模式被选中
                    giftManager.showGiftOnce(MainApplication.myName, curSelectGift)
                    giftDialog.dismiss()
                } else if (dialogBinding.radioSendDouble.isChecked) {        //连击模式被选中
                    giftManager.showGiftDouble(MainApplication.myName, curSelectGift)
                }
            }
            R.id.btn_gift1 -> {
                setGiftSelectState(0)       //点击第一个礼物
            }
            R.id.btn_gift2 -> {
                setGiftSelectState(1)       //点击第二个礼物
            }
            R.id.btn_gift3 -> {
                setGiftSelectState(2)       //以此类推
            }
            R.id.btn_gift4 -> {
                setGiftSelectState(3)
            }
            R.id.btn_gift5 -> {
                setGiftSelectState(4)
            }
            R.id.btn_gift6 -> {
                setGiftSelectState(5)
            }
            R.id.btn_gift7 -> {
                setGiftSelectState(6)
            }
            R.id.btn_gift8 -> {
                setGiftSelectState(7)
            }
            //屏幕其他区域, 双击点赞
            else -> {
                val currentTime = System.currentTimeMillis()        //获取当前时间
                if (currentTime - latestClickTime <= 400) {     //连击屏幕其他区域展示出点赞动画，最大间隔为400ms
                    praiseController.showPraise(binding.btnGift.x, binding.btnGift.y)
                }
                latestClickTime = currentTime       //更新最近一次的屏幕点击时间
            }
        }
    }

    /**
    * @Description: 被选中的礼物背景会发生变化，上一次被选中的礼物背景还原，更新当前存储的被选中礼物下标
    * @Param: giftIndex新选中的礼物下标
    */
    private fun setGiftSelectState(giftIndex: Int) {
        if (curSelectGift != giftIndex){
            giftViewList[curSelectGift].setBackgroundResource(android.R.color.transparent)
            giftViewList[giftIndex].setBackgroundResource(android.R.color.darker_gray)
            curSelectGift = giftIndex
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
                viewModel.stopLoading()     //停止加载评论
                item.setTitle(R.string.menu_item_start_comment_loading)
            } else {
                viewModel.startLoading()    //开始加载评论
                item.setTitle(R.string.menu_item_stop_comment_loading)
            }
        }
        return true
    }


    override fun startWorking() {
        isWorking = true
        startPlaying()
    }

    override fun stopWorking() {
        player.pause()
        isWorking = false
    }

    fun startPlaying(){
        if(this::player.isInitialized && isWorking && !binding.surfaceView.holder.isCreating){
            player.start()
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

    override fun onStop() {
        stopWorking()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}