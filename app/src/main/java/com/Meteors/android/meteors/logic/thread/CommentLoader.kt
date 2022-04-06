package com.Meteors.android.meteors.logic.thread

import android.os.HandlerThread

class CommentLoader: HandlerThread("CommentLoader") {
    
    /**
     * 创建Handler
     */
    override fun onLooperPrepared() {
        super.onLooperPrepared()
    }
    override fun quit(): Boolean {
        return super.quit()
    }

    /**
    * @Description: 不断产生评论数据
    */
    override fun run() {
        super.run()

    }
}