package com.Meteors.android.meteors

import android.app.Application

class MainApplication: Application() {

    companion object{
        //定义全局变量
        var myId = "user_default"
        var myName = "FanJiaxing"         //当前用户Id
    }

    override fun onCreate() {
        super.onCreate()
    }
}