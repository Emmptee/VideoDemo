package com.yoyo.mediacodec.codec;

import android.media.*;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoDecoder {
    private static final String TAG = "VideoDecoder";
    private static final long TIMEOUT_US = 12000;
    private DecoderInterface callBack;
    private VideoThread videoThread;
    private AudioThread audioThread;
    private boolean isRunning, isVideoDecodeOVer, isAudioDecodeOVer;
    private String filePath;
    private String threadName;

    public VideoDecoder(String filePath, String name) {
        this.filePath = filePath;
        this.threadName = name;
    }

    public void setCallBack(DecoderInterface callBack) {
        this.callBack = callBack;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void start() {
        isRunning = true;
        if (videoThread == null) {
            videoThread = new VideoThread();
            videoThread.start();
        }
        if (audioThread == null) {
            audioThread = new AudioThread();
            audioThread.start();
        }
    }

    public void destroy() {
        isRunning = false;
        if (audioThread != null){
            audioThread.interrupt();
        }
        if (videoThread != null){
            videoThread.interrupt();
        }
    }

    public void notifyEnd(){
        if(isVideoDecodeOVer && isAudioDecodeOVer){
            callBack.decodedEnd(filePath);
        }
    }

    private MediaExtractor configExtractor(String path){
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return extractor;
    }


    /*将缓冲区传递至解码器
    * 如果到了文件末尾，返回true;否则返回false
    */
    private boolean putBufferToCoder(MediaExtractor extractor, MediaCodec decoder, ByteBuffer[] inputBuffers) {
        boolean isMediaEOS = false;
        int inputBufferIndex = decoder.dequeueInputBuffer(TIMEOUT_US);

        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            int sampleSize = extractor.readSampleData(inputBuffer, 0);
            if (sampleSize < 0) {
                decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                isMediaEOS = true;
            } else {
                decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                extractor.advance();
            }
        }
        return isMediaEOS;
    }



    //获取指定类型媒体文件所在轨道
    private int getMediaTrackIndex(MediaExtractor videoExtractor, String MEDIA_TYPE) {
        int trackIndex = -1;
        for (int i = 0; i < videoExtractor.getTrackCount(); i++) {          
            MediaFormat mediaFormat = videoExtractor.getTrackFormat(i);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith(MEDIA_TYPE)) {
                trackIndex = i;
                break;
            }
        }
        return trackIndex;
    }



    private class VideoThread extends Thread {

        int videoTrackIndex;
        MediaCodec videoDecodeCodec;
        ByteBuffer[] inputBuffers, outputBuffers;
        MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
        private MediaExtractor videoExtractor;

        public VideoThread(){
            this.setName(threadName);
        }

        int width;
        int height;

        private void configVideoDecodeCodec(MediaFormat mediaFormat){
//            mediaFormat.setInteger(MediaFormat.KEY_WIDTH,mediaFormat.getInteger(MediaFormat.KEY_HEIGHT));
//            mediaFormat.setInteger(MediaFormat.KEY_HEIGHT,mediaFormat.getInteger(MediaFormat.KEY_WIDTH));
//            mediaFormat.setInteger("rotation-degrees",0);
            width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
            height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
            float time = mediaFormat.getLong(MediaFormat.KEY_DURATION) / 1000000;
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            callBack.decodedVideoInfo(filePath, width, height, time);

            try {
                videoDecodeCodec = MediaCodec.createDecoderByType(mediaFormat.getString(MediaFormat.KEY_MIME));
            } catch (IOException e) {
                e.printStackTrace();
            }
            videoDecodeCodec.configure(mediaFormat, null, null, 0);
            videoDecodeCodec.start();

            inputBuffers = videoDecodeCodec.getInputBuffers();
            outputBuffers = videoDecodeCodec.getOutputBuffers();
        }

        private void processCodecOutput(){
            int outputBufferIndex = videoDecodeCodec.dequeueOutputBuffer(videoBufferInfo, TIMEOUT_US);
            if(outputBufferIndex >= 0) {
                Image image = videoDecodeCodec.getOutputImage(outputBufferIndex);
                Log.e(TAG,"W:"+image.getWidth()+",,H:"+image.getHeight());
                byte[] nv21 = ImageUtils.getDataFromImage(image, ImageUtils.COLOR_FormatNV21);
                byte[] nv12 = new byte[width*height*3/2];
                ImageNative.NV21ToNV12(nv21, nv12, width, height);
                callBack.decodedByteBuffer(filePath, new MyFrame(nv12, width, height), outputBufferIndex);

                videoDecodeCodec.releaseOutputBuffer(outputBufferIndex, false);
            }
        }

        private void selfStop(){
            videoDecodeCodec.stop();
            videoDecodeCodec.release();
            videoExtractor.release();
        }

        @Override
        public void run() {
            videoExtractor = configExtractor(filePath);
            int videoTrackIndex = getMediaTrackIndex(videoExtractor, "video/");
            if (videoTrackIndex < 0) {
                return;
            }
            videoExtractor.selectTrack(videoTrackIndex);
            MediaFormat mediaFormat = videoExtractor.getTrackFormat(videoTrackIndex);
            configVideoDecodeCodec(mediaFormat);

            while (!Thread.interrupted()) {
                if (!isRunning) {
                    break;
                }
                putBufferToCoder(videoExtractor, videoDecodeCodec, inputBuffers);
                processCodecOutput();
                if ((videoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    isVideoDecodeOVer = true;
                    notifyEnd();
                    Log.e(TAG, "VideoThread buffer stream end");
                    break;
                }
            }//end while
            selfStop();
        }
    }

    long currentSampTime;

    private class AudioThread extends Thread {
        private int audioInputBufferSize;

        private AudioTrack audioTrack;
        private MediaCodec audioDecodeCodec;
        private MediaExtractor audioExtractor;
        MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();
        ByteBuffer[] inputBuffers;
        ByteBuffer[] outputBuffers;

        private int audioTrackIndex;

        private void configAudioTrack(MediaFormat mediaFormat){
            int audioChannels = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            int audioSampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int minBufferSize = AudioTrack.getMinBufferSize(audioSampleRate,
                    (audioChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
                    AudioFormat.ENCODING_PCM_16BIT);
            int maxInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
            audioInputBufferSize = minBufferSize > 0 ? minBufferSize * 4 : maxInputSize;
            int frameSizeInBytes = audioChannels * 2;
            audioInputBufferSize = (audioInputBufferSize / frameSizeInBytes) * frameSizeInBytes;
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    audioSampleRate,
                    (audioChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
                    AudioFormat.ENCODING_PCM_16BIT,
                    audioInputBufferSize,
                    AudioTrack.MODE_STREAM);
            audioTrack.play();
        }

        private void configAudioDecodeCodec(MediaFormat mediaFormat){
            try {
                String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                audioDecodeCodec = MediaCodec.createDecoderByType(mime);
            } catch (IOException e) {
                e.printStackTrace();
            }
            audioDecodeCodec.configure(mediaFormat, null, null, 0);
            audioDecodeCodec.start();

            inputBuffers = audioDecodeCodec.getInputBuffers();
            outputBuffers = audioDecodeCodec.getOutputBuffers();
        }

        private void selfStop(){
            audioDecodeCodec.stop();
            audioDecodeCodec.release();
            audioExtractor.release();
            if(null != audioTrack) {
                audioTrack.stop();
                audioTrack.release();
            }
        }

        private void processCoderOutput(){
            int outputBufferIndex = audioDecodeCodec.dequeueOutputBuffer(audioBufferInfo, TIMEOUT_US);
            switch (outputBufferIndex) {
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    Log.v(TAG, "format changed");
                    break;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    Log.v(TAG, "超时");
                    break;
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    outputBuffers = audioDecodeCodec.getOutputBuffers();
                    Log.v(TAG, "output buffers changed");
                    break;
                default:
                    ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                    //如果缓冲区里的可展示时间>当前视频播放的进度，就休眠一下

                    if (audioBufferInfo.size > 0) {
                        byte[] mAudioOutTempBuf = new byte[audioBufferInfo.size];
                        outputBuffer.position(0);
                        outputBuffer.get(mAudioOutTempBuf, 0, audioBufferInfo.size);
                        outputBuffer.clear();

                        if (audioTrack != null){
                            audioTrack.write(mAudioOutTempBuf, 0, audioBufferInfo.size);
                        }
                        Log.e(TAG, "audio buffer " +  audioBufferInfo.presentationTimeUs);
                        callBack.decodedAudioBuffer(filePath, new AudioPackage(mAudioOutTempBuf, currentSampTime));
                    }
                    audioDecodeCodec.releaseOutputBuffer(outputBufferIndex, false);
                    break;
            }

        }

        @Override
        public void run() {
            audioExtractor = configExtractor(filePath);
            //获取音频所在轨道
            audioTrackIndex = getMediaTrackIndex(audioExtractor, "audio/");

            if (audioTrackIndex < 0) {
                return;
            }

            MediaFormat mediaFormat = audioExtractor.getTrackFormat(audioTrackIndex);
            audioExtractor.selectTrack(audioTrackIndex);

//            configAudioTrack(mediaFormat);
            configAudioDecodeCodec(mediaFormat);

            long startMs = System.currentTimeMillis();

            while (!Thread.interrupted()) {
                if (!isRunning) {
                    break;
                }

                putBufferToCoder(audioExtractor, audioDecodeCodec, inputBuffers);
//                sleepRender(audioBufferInfo, startMs);
                processCoderOutput();

                if ((audioBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.e(TAG, "AudioThread buffer stream end");
                    isAudioDecodeOVer = true;
                    notifyEnd();
                    break;
                }
            }
            selfStop();
        }
    }


    //延迟渲染
    private void sleepRender(MediaCodec.BufferInfo audioBufferInfo, long startMs) {
        while (audioBufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }

}
