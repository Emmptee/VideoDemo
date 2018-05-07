/* DO NOT EDIT THIS FILE - it is machine generated */

#include <stdio.h>
#include "com_yoyo_mediacodec_codec_ImageNative.h"
#include "yuvconvert.h"
#include <string.h>

#include<android/log.h>

#define TAG "JNI" // ������Զ����LOG�ı�ʶ
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG ,__VA_ARGS__) // ����LOGE����

/*
 * Class:     com_yoyo_mediacodec_codec_ImageNative
 * Method:    test
 * Signature: ([B[B)V
 */
JNIEXPORT jbyteArray JNICALL Java_com_yoyo_mediacodec_codec_ImageNative_test
  (JNIEnv *env, jclass javaClass, jbyteArray javaBytes, jint srcWidth, jint srcHeight, jint destWidth, jint destHeight){
    jbyte* buffer1 = (*env)->GetByteArrayElements(env, javaBytes, NULL);

    for(int i=0; i<10; i++){
      buffer1[i] = 6;
    }

    (*env)->ReleaseByteArrayElements(env, javaBytes, buffer1, 0);

    jbyteArray array = (*env)->NewByteArray(env, 10);
    (*env)->SetByteArrayRegion(env, array, 0, 10, buffer1);
  }


JNIEXPORT jboolean JNICALL Java_com_yoyo_mediacodec_codec_ImageNative_YUVScale
  (JNIEnv *env, jclass javaClass, jbyteArray srcBytes, jint srcWidth, jint srcHeight, jbyteArray destBytes, jint destWidth, jint destHeight){

      jbyte* src = (*env)->GetByteArrayElements(env, srcBytes, NULL);
      jbyte* dest = (*env)->GetByteArrayElements(env, destBytes, NULL);

//      for(int i=0; i<10; i++){
//        dest[i] = src[i];
//      }

     unsigned char *rgb = (unsigned char *)malloc(srcWidth*srcHeight*3);
     unsigned char *rgbDest = (unsigned char *)malloc(destWidth*destHeight*3);

      Yuv420ToRgbConvert(src, rgb, srcWidth, srcHeight);

      ZoomBitMap(rgb, rgbDest, srcWidth,srcHeight, (float)destWidth/srcWidth, (float)destHeight/srcHeight);

//      rgbCompress(rgb, srcWidth, srcHeight, rgbDest, destWidth, destHeight);

      Rgb2YuvConvert(rgbDest, dest, destWidth, destHeight);

      free(rgb);
      free(rgbDest);

      (*env)->ReleaseByteArrayElements(env, srcBytes, src, 0);
      (*env)->ReleaseByteArrayElements(env, destBytes, dest, 0);

//      jbyteArray array = (*env)->NewByteArray(env, 10);
//      (*env)->SetByteArrayRegion(env, array, 0, 10, buffer1);
  }

  JNIEXPORT void JNICALL Java_com_yoyo_mediacodec_codec_ImageNative_NV21ToNV12
    (JNIEnv *env, jclass javaClass, jbyteArray srcBytes, jbyteArray destBytes, jint width, jint height){
        jbyte* nv21 = (*env)->GetByteArrayElements(env, srcBytes, NULL);
        jbyte* nv12 = (*env)->GetByteArrayElements(env, destBytes, NULL);

        int framesize = width*height;
        int i = 0,j = 0;
        memcpy(nv12, nv21, framesize);
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

        (*env)->ReleaseByteArrayElements(env, srcBytes, nv21, 0);
        (*env)->ReleaseByteArrayElements(env, destBytes, nv12, 0);
    }