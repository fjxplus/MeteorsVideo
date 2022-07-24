# MeteorsVideo(后端服务器到期不能使用了)
软件大体两个功能：(增加了ViewPager2，可以左右滑动啦)

1. 刷短视频功能：

   - （收藏）导航栏下播放assets目录下的短视频

   - （推荐目录下）播放网络服务器拉取的视频（可以叫我临时增加服务器带宽，平时的话服务器带宽很低只有1M， 建议将带宽增加到5-10M，或者使用本地服务器）

   -  单页面功能包括点赞、评论、上下滑动切换视频以及下拉刷新

2. 模拟直播功能：
   - 连击屏幕点赞

   - 礼物赠送

   - 评论区和动态公屏

## 部分截图

![短视频-视频界面](https://github.com/fjxplus/MeteorsVideo/blob/main/Screenshoot/Screenshoot_1.jpg)

![短视频-评论界面](https://github.com/fjxplus/MeteorsVideo/blob/main/Screenshoot/Screenshoot_2.jpg)

![直播-点赞界面](https://github.com/fjxplus/MeteorsVideo/blob/main/Screenshoot/Screenshoot_3.jpg)

![直播-礼物界面](https://github.com/fjxplus/MeteorsVideo/blob/main/Screenshoot/Screenshoot_4.jpg)

![直播-礼物动画界面](https://github.com/fjxplus/MeteorsVideo/blob/main/Screenshoot/Screenshoot_5.jpg)

## 一、View层

### 短视频

1. RecyclerView：使用RecyclerView展示视频列表，单页面只显示一张视图（使用PagerSnapHelper工具类）， RecyclerView主要监听视图切换的状态（情况较复杂，要对不同的滑动状态进行判断），并控制视频的播放和暂停。
2. SurfaceView：单页面使用MediaPlayer对视频数据进行解析，SurfaceView对数据进行渲染， 监听SurfaceView的生命周期回调，控制视频播放。
3. MediaPlayer：由于每个页面均使用一个MediaPlayer进行数据解析，所以设计一个MediaPlayerPool对所有的MediaPlayer进行LRU缓存管理，并提供加载、播放、暂停、回收等方法。
   - 当进行加载请求时，预缓存后面的几个MediaPlayer；
   - 当视图被回收时，释放MediaPlayer占用的资源；
   - 由于需要和SurfaceView的状态进行配合，所以要注意它们初始状态的判断。
4. 点赞：使用MotionLayout实现简单的动画，绘制不同状态的scene并在代码中触发动画
5. 评论区：使用PopUpWindow（还可以使用Dialog）实现一个单独评论区覆盖的效果（不会压缩视图），使用补间动画实现弹出动画，内置RecyclerView列表显示评论。
6. 头像：所有的头像均使用Glide（快）向服务器拉取。

### 直播间

1. 点赞：直播间的点赞动画使用属性动画来完成。
   - 属性动画：自定义一个ImageView，ObjectAnimator和评估程序来完成贝塞尔曲线的移动动画（并进行随机偏移），并定义一些插值器实现缩放和透明度的控制。
   - Pool的使用：由于我们连击屏幕，大量的View对象被创建并添加到视图中，所以设计一个常量池模型，需要使用View时从常量池Pool中取出，动画完成时放回常量池， 这样可以避免内存抖动造成的卡顿。
   - 随机更换点赞的图标
2. 评论区公屏：线程和Handler的使用
   - 开启一个专用线程，实时地拉取公屏的数据（由于没有后端数据支撑，随机模拟了几个评论）
   - 主线程和子线程共享使用RecyclerView的数据源ArrayList，为了避免造成错误，应当对临近资源进行同步控制（synchronized）
   - 线程之间使用Handler进行线程通信，主线程可以控制子线程是否工作，子线程通知主线程刷新评论区视图。
   - 线程的管理：视图销毁时线程也应同时销毁，释放资源。可以将线程设置为生命周期感知线程，感知Fragment生命周期；也可以在ViewModel或者仓库中管理线程。
3. 礼物中心：使用Dialog展示的礼物中心，搭配属性动画完成赠送
   - Dialog：设置LayoutParams， 指定在屏幕底部展示，添加视图即可。
   - 礼物展示动画：在屏幕左放几个View作为动画位，需要时展示动画就好了，什么动画都行。由于动画位置有限，抢占式。
   - 礼物中心：目前只是一个简单的静态界面，展示了礼物图片和两个RadioButton

## 二、ViewModel层

我们使用ViewModel（类）实现ViewModel（视图模型），作为一个Model和View之间的连接桥梁。

1. 请求数据：提供了向仓库中获取数据的方法，使用MutableLiveData触发网络请求，然后转换为LiveData在View层进行监听。
2. 对UI数据进行保存，可以防止设备配置改变造成的UI数据丢失。
3. 在onClear（）中释放一些资源。


## 三、Model层

Model是软件的模型层，定义了数据模型和数据来源，这里我们对数据来源并不关心，交给仓库类进行判断和管理，为我们提供获取数据的方法。
View层获取数据的方式就变为：View --- ViewModel --- Repository --- NetWork / Local

1. 定义各种数据类
1. Retrofit：向服务器获取视频列表，视频，评论数据，头像等等。
   - 创建网络接口
   - 使用RetrofitBuilder构建网络接口的代理对象
   - NetWork类：使用协程对网络请求Call的回调进行封装
   - Repository：使用LiveData构建协程域，并封装网络请求的ResponseBody，交给ViewModel层进行处理。
