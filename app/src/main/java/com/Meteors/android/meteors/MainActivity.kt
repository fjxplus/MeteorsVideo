package com.Meteors.android.meteors

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.Meteors.android.meteors.databinding.ActivityMainBinding
import com.Meteors.android.meteors.logic.model.VideoResponse
import com.Meteors.android.meteors.ui.LiveVideo.LiveVideoFragment
import com.Meteors.android.meteors.ui.ShortVideo.assets.AssetsVideoFragment
import com.Meteors.android.meteors.ui.ShortVideo.net.NetVideoFragment


private const val TAG = "Meteors_MainActivity"
private const val TAG_FRAGMENT_RECOMMEND = "fragment_recommend"
private const val TAG_FRAGMENT_LIVE = "fragment_live"
private const val TAG_FRAGMENT_COLLECT = "fragment_collect"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolBar)
        //添加主界面的Fragment
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment == null) {
            val fragment = AssetsVideoFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, fragment, TAG_FRAGMENT_RECOMMEND)
                .commit()
        }

        //监听底部导航栏点击事件
        binding.navigationBottom.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.collect -> {
                    Log.d(TAG, "clicked 收藏")
                    if (supportFragmentManager.findFragmentByTag(TAG_FRAGMENT_RECOMMEND) == null) {
                        // 添加推荐界面的Fragment
                        val fragment = AssetsVideoFragment()
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, fragment, TAG_FRAGMENT_RECOMMEND)
                            .commit()
                    }
                }

                R.id.recommend -> {
                    Log.d(TAG, "clicked 推荐")
                    if (supportFragmentManager.findFragmentByTag(TAG_FRAGMENT_COLLECT) == null) {
                        // 添加推荐界面的Fragment
                        val fragment = NetVideoFragment()
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, fragment, TAG_FRAGMENT_COLLECT)
                            .commit()
                    }
                }
                R.id.live -> {
                    Log.d(TAG, "clicked 直播")
                    if (supportFragmentManager.findFragmentByTag(TAG_FRAGMENT_LIVE) == null) {
                        // 添加推荐界面的Fragment
                        val fragment = LiveVideoFragment.newInstance(
                            VideoResponse(
                                "VID_2",
                                "FanJiaxing",
                                "兴小范",
                                "双11大促"
                            )
                        )
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, fragment, TAG_FRAGMENT_LIVE)
                            .commit()
                    }
                }
            }
            true
        }
        //监听底部导航栏重新点击事件
        binding.navigationBottom.setOnItemReselectedListener {
            Log.d(TAG, "bottom navigation reselected")
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }
}