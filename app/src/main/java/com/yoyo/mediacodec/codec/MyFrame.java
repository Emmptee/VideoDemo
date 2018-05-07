package com.yoyo.mediacodec.codec;

/**
 * Created by yoyo on 2018/3/16.
 */
public class MyFrame {

    private byte[] bytes;
    int width, height;

    public MyFrame(byte[] bytes, int width, int height){
        this.bytes = bytes;
        this.height = height;
        this.width = width;
    }

    public byte[] getBytes(){
        return this.bytes;
    }

    public int getWidth(){
        return width;
    }

    public int getHeight(){
        return height;
    }
}
