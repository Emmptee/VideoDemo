LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := ImageNative
LOCAL_SRC_FILES := com_yoyo_mediacodec_codec_ImageNative.c yuvconvert.c
LOCAL_LDLIBS :=-llog
include $(BUILD_SHARED_LIBRARY)