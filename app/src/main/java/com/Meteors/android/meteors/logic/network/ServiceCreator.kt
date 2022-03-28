package com.Meteors.android.meteors.logic.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ServiceCreator {

    private const val BASE_URL = "http://127.0.0.1/meteors/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun <T> create(serviceClass: Class<T>):T = retrofit.create(serviceClass)

    // 获取Retrofit网络请求接口的动态代理对象
    inline fun <reified T>create(): T = create(T::class.java)
}