/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_yoyo_mediacodec_codec_ImageNative */

#ifndef _Included_com_yoyo_mediacodec_codec_ImageNative
#define _Included_com_yoyo_mediacodec_codec_ImageNative
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_yoyo_mediacodec_codec_ImageNative
 * Method:    test
 * Signature: ([BIIII)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_yoyo_mediacodec_codec_ImageNative_test
  (JNIEnv *, jclass, jbyteArray, jint, jint, jint, jint);

/*
 * Class:     com_yoyo_mediacodec_codec_ImageNative
 * Method:    YUVcut
 * Signature: ([BII[BII)Z
 */
JNIEXPORT jboolean JNICALL Java_com_yoyo_mediacodec_codec_ImageNative_YUVScale
  (JNIEnv *, jclass, jbyteArray, jint, jint, jbyteArray, jint, jint);

/*
 * Class:     com_yoyo_mediacodec_codec_ImageNative
 * Method:    NV21ToNV12
 * Signature: ([B[BII)V
 */
JNIEXPORT void JNICALL Java_com_yoyo_mediacodec_codec_ImageNative_NV21ToNV12
  (JNIEnv *, jclass, jbyteArray, jbyteArray, jint, jint);

#ifdef __cplusplus
}
#endif
#endif