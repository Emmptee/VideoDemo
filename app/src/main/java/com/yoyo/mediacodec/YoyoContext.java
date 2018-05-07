package com.yoyo.mediacodec;

import com.yoyo.mediacodec.codec.AudioPackage;
import com.yoyo.mediacodec.codec.MyFrame;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by yoyo on 2018/3/14.
 */
public class YoyoContext {

    public static final String TAG = "YOYO";

    //待解码视频缓冲队列，静态成员！
    public static ArrayBlockingQueue<MyFrame> YUVQueue1 = new ArrayBlockingQueue<MyFrame>(20);
    public static ArrayBlockingQueue<MyFrame> YUVQueue2 = new ArrayBlockingQueue<MyFrame>(20);

    public static ArrayBlockingQueue<AudioPackage> audioQueue = new ArrayBlockingQueue<AudioPackage>(20);
}
