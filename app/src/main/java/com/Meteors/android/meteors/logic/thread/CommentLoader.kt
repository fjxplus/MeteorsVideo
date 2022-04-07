package com.Meteors.android.meteors.logic.thread

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import com.Meteors.android.meteors.logic.model.Comment
import kotlin.random.Random

private const val MESSAGE_LOADING = 0
private const val MESSAGE_UPDATE = 0

class CommentLoader(private val comments: ArrayList<Comment>, private val updateHandler: Handler) :
    HandlerThread("CommentLoader") {

    /**
     * 控制评论区的下载
     */
    private var isWorking = false

    /**
     * 控制下载功能的Handler， 接收MESSAGE_LOADING的Message
     */
    private lateinit var requestHandler: Handler

    /**
     * 模拟评论，随机选取发送
     */
    private val randomComments: List<Comment> = listOf(
        Comment("user100", "哔哔哩哩", "诶呦，不错哦！"),
        Comment("user101", "百百度度", "有事找我"),
        Comment("user102", "跳跳动动", "代码和人，有一个能跑就行"),
        Comment("user103", "啊吧啊吧", "啊吧啊吧啊吧啊吧"),
        Comment("user104", "惊动", "恭喜你已经毕业"),
        Comment("user105", "马牛x", "666666"),
        Comment("user106", "Android开发者", "牛啊牛啊"),
        Comment("user107", "working", "开摆开摆"),
        Comment("user108", "产品经理", "哎我， 这年轻人")
    )

    /**
     * 外部调用开始进行评论的获取， 将加载任务交给Handler
     */
    fun startLoading() {
        requestHandler.obtainMessage(MESSAGE_LOADING).sendToTarget()
    }

    /**
     * 外部调用，停止加载
     */
    fun stopLoading() {
        isWorking = false
    }

    /**
     * @Description: 根据isWorking变量，无限循环获取Comment， 这里进行模拟，随机产生评论数据，发送给主线程进行UI更新
     */
    private fun loading() {
        if (isWorking) {
            while (isWorking) {
                synchronized(comments) {
                    comments.add(randomComments[Random.nextInt(0, 9)])
                    updateHandler.obtainMessage(MESSAGE_UPDATE).sendToTarget()
                }
                Thread.sleep((Random.nextInt(5, 50) * 100).toLong())
            }
        }
    }

    /**
     * @Description: 创建子线程的Handler，注意调用looper会让主线程处于阻塞状态， 直至looper加载完成
     */
    override fun start() {
        super.start()
        //此时仍然处于主线程
        requestHandler = object : Handler(looper) {
            override fun handleMessage(msg: Message) {
                if (msg.what == MESSAGE_LOADING) {
                    isWorking = true
                    loading()
                }
            }
        }

    }

    /**
     * @Description: 停止加载，回收线程
     */
    override fun quit(): Boolean {
        isWorking = false
        return super.quit()
    }

}