package com.yoyo.mediacodec.codec;

/**
 * Created by yoyo on 2018/3/14.
 */
public interface DecoderInterface {
    void decodedByteBuffer(String path, MyFrame frame, int bufferIndex);
    void decodedVideoInfo(String path, int width, int height, float duration);
    void decodedEnd(String path);
    void decodedAudioBuffer(String path,AudioPackage audioPackage);
}