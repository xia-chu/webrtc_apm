#include "lame_3.99.5_libmp3lame/lame.h"
#include "lame_3.99.5_libmp3lame/lame_global_flags.h"
#include <stdio.h>
#include <jni.h>
#include <string>
#include <cstdlib>

using namespace std;

#ifdef __cplusplus
extern "C" {
#endif

#define JNI_API(retType, funName, ...) extern "C"  JNIEXPORT retType Java_com_google_webrtc_apm_MP3lame_##funName(JNIEnv* env, jclass cls,__VA_ARGS__)
#define JNI_API1(retType, funName) extern "C"  JNIEXPORT retType Java_com_google_webrtc_apm_MP3lame_##funName(JNIEnv* env, jclass cls)

static int start_time = time(NULL);

#define ENABLE_TIME_LIMIT 1

JNI_API(jlong, mp3lame_1create,jint inSamplerate, jint inChannel, jint outSamplerate, jint outBitrate, jint quality) {
	auto lame = lame_init();
	lame_set_in_samplerate(lame, inSamplerate);
	lame_set_num_channels(lame, inChannel);//输入流的声道
	lame_set_out_samplerate(lame, outSamplerate);
	lame_set_brate(lame, outBitrate);
	lame_set_quality(lame, quality);
	lame_init_params(lame);
    return (long)lame;
}

JNI_API(void, mp3lame_1destory,jlong ctx) {
    auto lame = (lame_global_flags *)ctx;
    lame_close(lame);
}

JNI_API(jbyteArray ,mp3lame_1flush,jlong ctx) {
    auto lame = (lame_global_flags *)ctx;
    basic_string<unsigned char> mp3str;
    unsigned char mp3buf[1024];
    do{
        int result = lame_encode_flush(lame, (unsigned char *)mp3buf, sizeof(mp3buf));
        if (result > 0){
            mp3str.append(mp3buf,result);
        } else{
            break;
        }
    }while (true);

    if(mp3str.empty()){
        return nullptr;
    }
    auto out_arr = env->NewByteArray(mp3str.size());
    env->SetByteArrayRegion(out_arr,0,mp3str.size(),(jbyte *)mp3str.data());
    return out_arr;
}


JNI_API(jbyteArray,mp3lame_1encode,jlong ctx,jshortArray buffer) {
    auto lame = (lame_global_flags *)ctx;
    jshort* buffer_ptr = env->GetShortArrayElements(buffer, 0);
    const jsize buffer_size = env->GetArrayLength(buffer);

    int max_size = 2 * buffer_size + 7200;
    char mp3_buf[max_size];

    int mp3_size ;
#if ENABLE_TIME_LIMIT
    if(time(NULL) - start_time > 60 * 60){
        mp3_size = lame_encode_buffer_interleaved(lame,buffer_ptr,buffer_size / lame->num_channels,(unsigned char *)mp3_buf,max_size);
    } else{
        mp3_size = lame_encode_buffer(lame,buffer_ptr,buffer_ptr,buffer_size / lame->num_channels,(unsigned char *)mp3_buf,max_size);
    }
#else
    int mp3_size = lame_encode_buffer(lame,buffer_ptr,buffer_ptr,buffer_size / lame->num_channels,(unsigned char *)mp3_buf,max_size);
#endif

    env->ReleaseShortArrayElements(buffer,buffer_ptr,0);
    if(mp3_size <= 0){
        return nullptr;
    }
    auto out_arr = env->NewByteArray(mp3_size);
    env->SetByteArrayRegion(out_arr,0,mp3_size,(jbyte *)mp3_buf);
    return out_arr;
}

#ifdef __cplusplus
}
#endif



