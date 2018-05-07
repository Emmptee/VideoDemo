package com.yoyo.mediacodec.codec;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by yoyo on 2018/3/22.
 */
public class ImageUtils {

    public static final int COLOR_FormatI420 = 1;
    public static final int COLOR_FormatNV21 = 2;

    public static final int FILE_TypeI420 = 1;
    public static final int FILE_TypeNV21 = 2;
    public static final int FILE_TypeJPEG = 3;

    /**
     * 暂时不支持纵向坐标的制定
     */
    public static void overlayYUV(byte[] dest, int lay_x, int destWidht, int destHeight, byte[] src, int srcWidth, int srcHeight){
        //copy y of 1
        for(int i=0; i<srcHeight; i++){
            for(int j=0; j<srcWidth; j++){
                dest[lay_x + i*destWidht + j] = src[i*srcWidth + j];
            }
        }
        //copy u v
        for(int i=0; i<srcHeight/2; i++){
            for(int j=0; j<srcWidth; j++){
                dest[lay_x + destHeight*destWidht + i*destWidht+j] = src[srcWidth*srcHeight + i*srcWidth + j];
            }
        }
    }

    public static  void yuvSetBackgroud(byte[] src, int m_width, int m_height, byte backgroundColor){
        for (int i = 0; i < m_height / 2; i++) {
            for (int j = 0; j < m_width; j++) {
                src[m_width * m_height + i * m_width + j] = backgroundColor;
            }
        }
    }

    public static void NV21ToNV12(byte[] nv21,byte[] nv12,int width,int height){
        if(nv21 == null || nv12 == null)return;
        int framesize = width*height;
        int i = 0,j = 0;
        System.arraycopy(nv21, 0, nv12, 0, framesize);
        for(i = 0; i < framesize; i++){
            nv12[i] = nv21[i];
        }
        for (j = 0; j < framesize/2; j+=2)
        {
            nv12[framesize + j-1] = nv21[j+framesize];
        }
        for (j = 0; j < framesize/2; j+=2)
        {
            nv12[framesize + j] = nv21[j+framesize-1];
        }
    }

    public static boolean isImageFormatSupported(Image image) {
        int format = image.getFormat();
        switch (format) {
            case ImageFormat.YUV_420_888:
            case ImageFormat.NV21:
            case ImageFormat.YV12:
                return true;
        }
        return false;
    }

    public static byte[] getDataFromImage(Image image, int colorFormat) {
        if (colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21) {
            throw new IllegalArgumentException("only support COLOR_FormatI420 " + "and COLOR_FormatNV21");
        }
        if (!ImageUtils.isImageFormatSupported(image)) {
            throw new RuntimeException("can't convert Image to byte array, format " + image.getFormat());
        }
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = width * height;
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height + 1;
                        outputStride = 2;
                    }
                    break;
                case 2:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = (int) (width * height * 1.25);
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height;
                        outputStride = 2;
                    }
                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();
//            if (VERBOSE) {
//                Log.v(TAG, "pixelStride " + pixelStride);
//                Log.v(TAG, "rowStride " + rowStride);
//                Log.v(TAG, "width " + width);
//                Log.v(TAG, "height " + height);
//                Log.v(TAG, "buffer size " + buffer.remaining());
//            }
            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }
        return data;
    }

    int rotateYUV420Degree180(byte[] dstyuv,byte[] srcdata, int imageWidth, int imageHeight)
    {

        int i = 0, j = 0;

        int index = 0;
        int tempindex = 0;

        int ustart = imageWidth *imageHeight;
        tempindex= ustart;
        for (i = 0; i <imageHeight; i++) {

            tempindex-= imageWidth;
            for (j = 0; j <imageWidth; j++) {

                dstyuv[index++] = srcdata[tempindex + j];
            }
        }

        int udiv = imageWidth *imageHeight / 4;

        int uWidth = imageWidth /2;
        int uHeight = imageHeight /2;
        index= ustart;
        tempindex= ustart+udiv;
        for (i = 0; i < uHeight;i++) {

            tempindex-= uWidth;
            for (j = 0; j < uWidth;j++) {

                dstyuv[index]= srcdata[tempindex + j];
                dstyuv[index+ udiv] = srcdata[tempindex + j + udiv];
                index++;
            }
        }
        return 0;
    }

    // 码率等级
    public enum Quality{
        LOW, MIDDLE, HIGH
    }

    public static int getBitrate(int mWidth, int mHeight, Quality quality) {
        int bitRate = (int)(mWidth * mHeight * 20 * 2 *0.07f);
        if(mWidth >= 1920 || mHeight >= 1920){
            switch (quality){
                case LOW:
                    bitRate *= 0.75;// 4354Kbps
                    break;
                case MIDDLE:
                    bitRate *= 1.1;// 6386Kbps
                    break;
                case HIGH:
                    bitRate *= 1.5;// 8709Kbps
                    break;
            }
        }else if(mWidth >= 1280 || mHeight >= 1280){
            switch (quality){
                case LOW:
                    bitRate *= 1.0;// 2580Kbps
                    break;
                case MIDDLE:
                    bitRate *= 1.4;// 3612Kbps
                    break;
                case HIGH:
                    bitRate *= 1.9;// 4902Kbps
                    break;
            }
        }else if(mWidth >= 640 || mHeight >= 640){
            switch (quality){
                case LOW:
                    bitRate *= 1.4;// 1204Kbps
                    break;
                case MIDDLE:
                    bitRate *= 2.1;// 1806Kbps
                    break;
                case HIGH:
                    bitRate *= 3;// 2580Kbps
                    break;
            }
        }
        return bitRate;
    }

}
