LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
# LOCAL_LDLIBS += -llog -landroid

# OpenMP mode : enable these flags to enable using OpenMP for parallel computation

LOCAL_CFLAGS += -UHAVE_CONFIG_H -DENABLE_FAAC
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_C_INCLUDES += $(realpath $(LOCAL_PATH)/libFaac/)

LOCAL_SRC_FILES := \
libFaac/aacquant.c	\
libFaac/channels.c	\
libFaac/frame.c	\
libFaac/midside.c	\
libFaac/util.c \
libFaac/backpred.c 	\
libFaac/fft.c		\
libFaac/huffman.c	\
libFaac/psychkni.c \
libFaac/bitstream.c	\
libFaac/filtbank.c	\
libFaac/ltp.c \
libFaac/tns.c \
AACEncoder.cpp \
faac_jni.cpp


LOCAL_MODULE := faac
LOCAL_SHARED_LIBRARIES := faac
include $(BUILD_SHARED_LIBRARY)