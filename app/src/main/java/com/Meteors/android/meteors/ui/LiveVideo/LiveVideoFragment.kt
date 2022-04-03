package com.Meteors.android.meteors.ui.LiveVideo

import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.Meteors.android.meteors.databinding.FragmentLiveBinding

private const val TAG = "Meteors_Live_Fragment"

/**
* @Description:  直播界面的Fragment，可以进行礼物的赠送
*/
class LiveVideoFragment : Fragment() {

    private var _binding: FragmentLiveBinding? = null

    private val binding get() = _binding!!

    private lateinit var player: MediaPlayer

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveBinding.inflate(inflater, container, false)

        player = MediaPlayer()
        val fd = requireContext().assets.openFd("VID_1.mp4")
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

        binding.surfaceView.holder.addCallback(object: SurfaceHolder.Callback{
            @RequiresApi(Build.VERSION_CODES.R)
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.d(TAG, "surfaceCreated")
                player.setDisplay(holder)

                player.start()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {  }

            override fun surfaceDestroyed(holder: SurfaceHolder) {  }
        })
        return binding.root
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
}