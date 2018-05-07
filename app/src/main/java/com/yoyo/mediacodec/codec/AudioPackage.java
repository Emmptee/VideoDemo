package com.yoyo.mediacodec.codec;

/**
 * Created by yoyo on 2018/3/21.
 */
public class AudioPackage {
    byte[] bytes;
    long sampeTime;

    public AudioPackage(byte[] b, long time){
        this.bytes = b;
        this.sampeTime = time;
    }

    public byte[] getBytes(){
        return bytes;
    }

    public long getSampeTime(){
        return this.sampeTime;
    }
}
