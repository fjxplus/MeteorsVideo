package com.Meteors.android.meteors

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.Meteors.android.meteors.databinding.ActivityMainBinding
import com.Meteors.android.meteors.logic.model.VideoResponse
import com.Meteors.android.meteors.ui.LiveVideo.LiveVideoFragment
import com.Meteors.android.meteors.ui.ShortVideo.assets.AssetsVideoFragment
import com.Meteors.android.meteors.ui.ShortVideo.net.NetVideoFragment

private const val TAG = "Meteors_MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val fragments: ArrayList<Fragment> = ArrayList()

    private lateinit var vpAdapter: Vp2FragmentStateAdapter

    private var currentFragment: Fragment? = null

    /**
    * @Description: 约定ViewPager中的Fragment工作状态
    */
    interface PlayerController{
        fun startWorking()
        fun stopWorking()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolBar)

        fragments.add(AssetsVideoFragment())
        fragments.add(NetVideoFragment())
        fragments.add(LiveVideoFragment.newInstance(
                VideoResponse(
                    "VID_live",
                    "FanJiaxing",
                    "兴小范",
                    "双11大促"
                )
            )
        )

        vpAdapter = Vp2FragmentStateAdapter(fragments, this)
        binding.viewPager.adapter = vpAdapter
        binding.viewPager.offscreenPageLimit = 3

        //为ViewPager添加监听器
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (currentFragment != null){
                    (currentFragment as PlayerController).stopWorking()     //停止上一个Fragment的工作
                }
                currentFragment = fragments[position]
                val controller = currentFragment as PlayerController
                controller.startWorking()       //开始当前Fragment动作
                binding.navigationBottom.menu.getItem(position).isChecked = true
                super.onPageSelected(position)
            }

            override fun onPageScrollStateChanged(state: Int) {
                when (state) {
                    ViewPager2.SCROLL_STATE_IDLE -> {       //滚动停止，开始当前播放
                        (currentFragment as PlayerController).startWorking()
                    }
                    ViewPager2.SCROLL_STATE_DRAGGING -> {   //开始滚动，暂停当前播放
                        (currentFragment as PlayerController).stopWorking()
                    }
                    else -> {}
                }
                super.onPageScrollStateChanged(state)
            }
        })

        //监听底部导航栏点击事件，切换ViewPager
        binding.navigationBottom.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.collect -> {
                    binding.viewPager.setCurrentItem(0, false)
                }

                R.id.recommend -> {
                    binding.viewPager.setCurrentItem(1, false)

                }
                R.id.live -> {
                    binding.viewPager.setCurrentItem(2, false)
                }
            }
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    /**
    * @Description: FragmentStateAdapter, ViewPager的Adapter，原理同RecyclerView
    */
    class Vp2FragmentStateAdapter(
        private val fragments: List<Fragment>,
        activity: FragmentActivity
    ) : FragmentStateAdapter(activity) {

        override fun createFragment(position: Int): Fragment {
            return fragments[position]
        }

        override fun getItemCount(): Int {
            return fragments.size
        }
    }
}