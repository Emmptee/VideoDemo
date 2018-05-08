package com.yoyo.mediacodec.codec;

import android.annotation.SuppressLint;
import android.media.*;
import android.os.Build;

import com.socks.library.KLog;
import com.yoyo.mediacodec.YoyoContext;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

import static android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
import static android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME;


public class AvcJoinEncoder {
    private final static String TAG = "AvcJoinEncoder";
    private int mVideoTrackIndex, mAudioTrackIndex;
    private int TIMEOUT_USEC = 12000;
    public static final int AUDIO_BUFFER_MAX_SIZE = 8192;

    private static final String AUDIO_ENCODE_MIME_TYPE = "audio/mp4a-latm";

    VideoThread videoThread;
    AudioThread audioThread;
    MediaCodec videoEncodeCodec, audioEncodeCodec;
    MediaMuxer mediaMuxer;
    public static boolean isAudioTrackAdded, isVideoTrackAdded, firstKeyFrameisAdd;

    ByteBuffer[] inputAudioBuffers, outputAudioBuffers;

    int m_width;
    int m_height;
    int m_framerate;
    int m_bitrate;

    byte backgroundColor = 127;
    private String path = "/mnt/sdcard/ffmpeg/test1.h264";

    public ArrayBlockingQueue<MyFrame> YUVQueue1;
    public ArrayBlockingQueue<MyFrame> YUVQueue2;

    public DecoderInterface decoderInterface;

    private MediaCodec.BufferInfo audioEncodeBufferInfo;

    public AvcJoinEncoder(int width, int height, int framerate, int bitrate, String path) {
        this.path = path;
        m_width  = width;
        m_height = height;
        m_framerate = framerate;
        m_bitrate = bitrate;
        new File(path).delete();
        configMediaMuxer();
    }

    private void configMediaMuxer(){
        try {
            mediaMuxer = new MediaMuxer(this.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int addTrackToMuxer(MediaFormat mediaFormat, boolean isVideo){
        int index = mediaMuxer.addTrack(mediaFormat);
        if(isVideo){
            isVideoTrackAdded = true;
        }else{
            isAudioTrackAdded = true;
        }
        if(isAudioTrackAdded && isVideoTrackAdded) {
            mediaMuxer.start();
        }
        return index;
    }

    public void configVideoEncodeCodec(){
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", m_width, m_height);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, m_bitrate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, m_framerate);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        try {
            videoEncodeCodec = MediaCodec.createEncoderByType("video/avc");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //配置编码器参数
        videoEncodeCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        //启动编码器
        videoEncodeCodec.start();
    }



    long generateIndex = 0;

    public void putVideoFrameToCodec(MyFrame frame1, MyFrame frame2){
        byte[] yuv420sp = new byte[m_width * m_height * 3 / 2];

        int offset = 0;

        int twidth = 640;
        int theight = 360;

        ImageUtils.yuvSetBackgroud(yuv420sp, m_width, m_height, backgroundColor);

        byte[] destf1 = new byte[twidth*theight*3/2];

        ImageNative.YUVScale(frame1.getBytes(), frame1.getWidth(), frame1.getHeight(), destf1, twidth, theight);

        byte[] destf2 = new byte[twidth*theight*3/2];

        ImageNative.YUVScale(frame2.getBytes(), frame2.getWidth(), frame2.getHeight(), destf2, twidth, theight);

        long start = System.currentTimeMillis();
        ImageUtils.overlayYUV(yuv420sp, offset, m_width, m_height, destf1, twidth, theight);
//        ImageUtils.overlayYUV(yuv420sp, frame1.getWidth() + offset, m_width, m_height, frame2.getBytes(), frame2.getWidth(), frame2.getHeight());
        ImageUtils.overlayYUV(yuv420sp, twidth + offset, m_width, m_height, destf2, twidth, theight);

        //编码器输入缓冲区
        ByteBuffer[] inputBuffers = videoEncodeCodec.getInputBuffers();
        //编码器输出缓冲区
        int inputBufferIndex = videoEncodeCodec.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            //把转换后的YUV420格式的视频帧放到编码器输入缓冲区中
            inputBuffer.put(yuv420sp);
            videoEncodeCodec.queueInputBuffer(inputBufferIndex, 0, yuv420sp.length, computePresentationTime(generateIndex), 0);
            generateIndex += 1;
        }
    }

    public void processVideoEncodeOut(){
        ByteBuffer[] outputBuffers = videoEncodeCodec.getOutputBuffers();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = videoEncodeCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
        while (outputBufferIndex >= 0) {
            ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];

            if(bufferInfo.flags == BUFFER_FLAG_CODEC_CONFIG){
                mVideoTrackIndex = addTrackToMuxer(videoEncodeCodec.getOutputFormat(), true);
            }else if(bufferInfo.flags == BUFFER_FLAG_KEY_FRAME){
                mediaMuxer.writeSampleData(mVideoTrackIndex, outputBuffer, bufferInfo);
                KLog.e(TAG, "mux avc I Frame end");
                firstKeyFrameisAdd = true;
            }else{
                mediaMuxer.writeSampleData(mVideoTrackIndex, outputBuffer, bufferInfo);
                KLog.e(TAG, "mux avc B Frame end");
            }
            videoEncodeCodec.releaseOutputBuffer(outputBufferIndex, false);

            outputBufferIndex = videoEncodeCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
        }
        KLog.e("编码结束========================");
    }


    public void configAudioEncodeCodec(){
        // 告诉编码器输出数据的格式,如MIME类型、码率、采样率、通道数量等
        MediaFormat mediaFormat = new MediaFormat();
        mediaFormat.setString(MediaFormat.KEY_MIME, AUDIO_ENCODE_MIME_TYPE);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE,128000);
        mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, 44100);
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT,2);
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,AUDIO_BUFFER_MAX_SIZE);

        try{
            MediaCodecInfo mCodecInfo = selectSupportCodec(AUDIO_ENCODE_MIME_TYPE);
            audioEncodeCodec = MediaCodec.createByCodecName(mCodecInfo.getName());
        }catch(IOException e){
            e.printStackTrace();
        }

        if(audioEncodeCodec != null){
            audioEncodeCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            audioEncodeCodec.start();
        }

        audioEncodeBufferInfo = new MediaCodec.BufferInfo();
        inputAudioBuffers = audioEncodeCodec.getInputBuffers();
        outputAudioBuffers = audioEncodeCodec.getOutputBuffers();
    }

    public void putAudioDataToCodec(AudioPackage audioPackage){
        byte[] audioBuf = audioPackage.getBytes();
        int readBytes = audioPackage.getBytes().length;

        //返回编码器的一个输入缓存区句柄，-1表示当前没有可用的输入缓存区
        int inputBufferIndex = audioEncodeCodec.dequeueInputBuffer(TIMEOUT_USEC);
        if(inputBufferIndex >= 0){
            // 绑定一个被空的、可写的输入缓存区inputBuffer到客户端
            ByteBuffer inputBuffer  = null;
            if(!isLollipop()){
                inputBuffer = inputAudioBuffers[inputBufferIndex];
            }else{
                inputBuffer = audioEncodeCodec.getInputBuffer(inputBufferIndex);
            }
            // 向输入缓存区写入有效原始数据，并提交到编码器中进行编码处理
            if(audioBuf==null || readBytes<=0){
//            	mAudioEncoder.queueInputBuffer(inputBufferIndex,0,0,getPTSUs(),MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                audioEncodeCodec.queueInputBuffer(inputBufferIndex, 0, 0, getPTSUs(audioPackage.getSampeTime()), MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            }else{
                inputBuffer.clear();
                inputBuffer.put(audioBuf);
                audioEncodeCodec.queueInputBuffer(inputBufferIndex, 0, readBytes, getPTSUs(audioPackage.getSampeTime()), 0);
//                mAudioEncoder.queueInputBuffer(inputBufferIndex,0,readBytes,getPTSUs(),0);
            }
        }
    }

    @SuppressLint("WrongConstant")
    public void processAudioEncodeOut(){
        int outputBufferIndex = -1;
        do{
            audioEncodeBufferInfo = new MediaCodec.BufferInfo();
            outputBufferIndex = audioEncodeCodec.dequeueOutputBuffer(audioEncodeBufferInfo,TIMEOUT_USEC);
            if(outputBufferIndex == MediaCodec. INFO_TRY_AGAIN_LATER){
//                    KLog.i(TAG,"获得编码器输出缓存区超时");
            }else if(outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){
                // 如果API小于21，APP需要重新绑定编码器的输入缓存区；
                // 如果API大于21，则无需处理INFO_OUTPUT_BUFFERS_CHANGED
                if(!isLollipop()){
                    outputAudioBuffers = audioEncodeCodec.getOutputBuffers();
                }
            }else if(outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){
                // 编码器输出缓存区格式改变，通常在存储数据之前且只会改变一次
                // 这里设置混合器视频轨道，如果音频已经添加则启动混合器（保证音视频同步）
                synchronized (mediaMuxer) {
                    mAudioTrackIndex = addTrackToMuxer(audioEncodeCodec.getOutputFormat(), false);
                }
            }else{
                // 当flag属性置为BUFFER_FLAG_CODEC_CONFIG后，说明输出缓存区的数据已经被消费了
                if((audioEncodeBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0){
                    audioEncodeBufferInfo.size = 0;
                }
                // 数据流结束标志，结束本次循环

                // 获取一个只读的输出缓存区inputBuffer ，它包含被编码好的数据
                ByteBuffer outputBuffer = null;
                if(!isLollipop()){
                    outputBuffer  = outputAudioBuffers[outputBufferIndex];
                }else{
                    outputBuffer  = audioEncodeCodec.getOutputBuffer(outputBufferIndex);
                }
                if(audioEncodeBufferInfo.size != 0){
                    // 获取输出缓存区失败，抛出异常
                    if(outputBuffer == null){
                        throw new RuntimeException("encodecOutputBuffer"+outputBufferIndex+"was null");
                    }
                    // 如果API<=19，需要根据BufferInfo的offset偏移量调整ByteBuffer的位置
                    //并且限定将要读取缓存区数据的长度，否则输出数据会混乱
                    if(isKITKAT()){
                        outputBuffer.position(audioEncodeBufferInfo.offset);
                        outputBuffer.limit(audioEncodeBufferInfo.offset+audioEncodeBufferInfo.size);
                    }
                    if(!firstKeyFrameisAdd) {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    time++;
                    audioEncodeBufferInfo.presentationTimeUs = time*BUFFER_DURATION_US;
                    mediaMuxer.writeSampleData(mAudioTrackIndex, outputBuffer, audioEncodeBufferInfo);
                    KLog.e(TAG, "mux audio end " + audioEncodeBufferInfo.presentationTimeUs);
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                // 处理结束，释放输出缓存区资源
                audioEncodeCodec.releaseOutputBuffer(outputBufferIndex,false);
                KLog.e("音频编码结束22222222");
            }
        }while (outputBufferIndex >= 0);
    }

    private void StopEncoder() {
        try {
            mediaMuxer.stop();
            mediaMuxer.release();
            mediaMuxer = null;

            videoEncodeCodec.stop();
            videoEncodeCodec.release();
            videoEncodeCodec = null;
            audioEncodeCodec.stop();
            audioEncodeCodec.release();
            audioEncodeCodec = null;
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public boolean isRuning = false;

    public void StopThread(){
        isRuning = false;
        audioThread.interrupt();
        videoThread.interrupt();
        StopEncoder();
    }

    long time;
    long BUFFER_DURATION_US = 1_000_000 * (4096 /4) / 44100;


    class AudioThread extends Thread{

        @Override
        public void run() {
            configAudioEncodeCodec();
            while (isRuning) {
                if(YoyoContext.audioQueue.size() > 0){
                    AudioPackage audioPackage = YoyoContext.audioQueue.poll();
                    byte[] audioBuf = audioPackage.getBytes();
                    if (audioBuf != null) {
                        try {
                            KLog.e(TAG, "sssss + " + audioBuf.length);
                            putAudioDataToCodec(audioPackage);
                            processAudioEncodeOut();
                            if((audioEncodeBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                                KLog.e(TAG, "===========audio end============");
                                break;
                            }
                        } catch (IllegalStateException e) {
                            // 捕获因中断线程并停止混合dequeueOutputBuffer报的状态异常
                            e.printStackTrace();
                        } catch (NullPointerException e) {
                            // 捕获因中断线程并停止混合MediaCodec为NULL异常
                            e.printStackTrace();
                        }
                    }
                }else{
//                exit();
                }
            }
        }
    }

    class VideoThread extends Thread{
        @Override
        public void run() {
            isRuning = true;
            configVideoEncodeCodec();
            while (isRuning) {

                long startMs = System.currentTimeMillis();

                if (null != YUVQueue1 && YUVQueue1.size() >0 && null != YUVQueue2 && YUVQueue2.size() >0) {
                    MyFrame frame1 = YUVQueue1.poll();
                    MyFrame frame2 = YUVQueue2.poll();
                    putVideoFrameToCodec(frame1, frame2);
                }
                processVideoEncodeOut();
            }
        }
    }
    public void StartEncoderThread(){
        videoThread = new VideoThread();
        videoThread.start();
        audioThread = new AudioThread();
        audioThread.start();
    }

    /**
     * Generates the presentation time for frame N, in microseconds.
     */
    private long computePresentationTime(long frameIndex) {
//        return 132 + frameIndex * 1000000 / m_framerate;
        return frameIndex * 1000000 / m_framerate;
    }

    private long prevPresentationTimes = 0;
    private long getPTSUs(long input){
//        if(result < prevPresentationTimes){
//        result = input + result;
//        }
        return input;
    }



    /**
     * 设置视频的空白背景
     * @param color  0为绿色   127(默认)为黑色，其他颜色自己测试
     */
    public void setBackgroundColor(byte color){
        backgroundColor = color;
    }

    /**
     * 遍历所有编解码器，返回第一个与指定MIME类型匹配的编码器
     *  判断是否有支持指定mime类型的编码器
     * */
    private MediaCodecInfo selectSupportCodec(String mimeType){
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            // 判断是否为编码器，否则直接进入下一次循环
            if (!codecInfo.isEncoder()) {
                continue;
            }
            // 如果是编码器，判断是否支持Mime类型
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    private boolean isLollipop(){
        // API>=21
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    private boolean isKITKAT(){
        // API<=19
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT;
    }
}