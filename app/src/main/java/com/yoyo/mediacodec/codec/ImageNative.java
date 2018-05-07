package com.yoyo.mediacodec.codec;

/**
 * Created by yoyo on 2018/3/22.
 */
public class ImageNative {

    static{
        System.loadLibrary("ImageNative");
    }

    native public static byte[] test(byte[] input, int srcWidth, int srcHeight, int destWidth, int destHeight);

    native public static boolean YUVScale(byte[] src, int srcWidth, int srcHeight, byte[] dest, int destWidth, int destHeight);

    native public static void NV21ToNV12(byte[] src, byte[] dest, int width, int height);

}
