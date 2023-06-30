package com.google.webrtc.apmdemo.file;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import com.google.webrtc.apm.Ticker;
import com.google.webrtc.apm.WebRtcJni;
import com.google.webrtc.apmdemo.BufferSlice;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by ZhangHao on 2017/5/17.
 * 用于aac音频解码
 */

public class AACDecoderUtil {
    private static final String TAG = "AACDecoderUtil";

    //WebRtcJni.WebRtcAecm aecm = new WebRtcJni.WebRtcAecm(KEY_SAMPLE_RATE,false,3);
    //声道数
    private static final int KEY_CHANNEL_COUNT = 2;
    //采样率
    private static final int KEY_SAMPLE_RATE = 16000;
    private static final int PCM_SLICE_MS = 20;
    //用于播放解码后的pcm
    private MyAudioTrack mPlayer;
    //解码器
    private MediaCodec mDecoder;
    //用来记录解码失败的帧数
    private int count = 0;

    BufferSlice bufferSlice = new BufferSlice(KEY_SAMPLE_RATE * PCM_SLICE_MS / 1000);


    WebRtcJni.WebRtcVad vad = new WebRtcJni.WebRtcVad(2);
    WebRtcJni.WebRtcNs ns = new WebRtcJni.WebRtcNs(KEY_SAMPLE_RATE,2);
    WebRtcJni.WebRtcAecm aecm = new WebRtcJni.WebRtcAecm(KEY_SAMPLE_RATE,false,3);
    WebRtcJni.WebRtcAgc agc = new WebRtcJni.WebRtcAgc(0,255,2,KEY_SAMPLE_RATE);



    /**
     * 初始化所有变量
     */
    public void start() {
        prepare();
    }

    /**
     * 初始化解码器
     *
     * @return 初始化失败返回false，成功返回true
     */
    public boolean prepare() {
        // 初始化AudioTrack
        mPlayer = new MyAudioTrack(KEY_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        mPlayer.init();
        try {
            //需要解码数据的类型
            String mine = "audio/mp4a-latm";
            //初始化解码器
            mDecoder = MediaCodec.createDecoderByType(mine);
            //MediaFormat用于描述音视频数据的相关参数
            MediaFormat mediaFormat = new MediaFormat();
            //数据类型
            mediaFormat.setString(MediaFormat.KEY_MIME, mine);
            //声道个数
            mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, KEY_CHANNEL_COUNT);
            //采样率
            mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, KEY_SAMPLE_RATE);
            //比特率
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
            //用来标记AAC是否有adts头，1->有
            mediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
            //用来标记aac的类型
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            //ByteBuffer key（暂时不了解该参数的含义，但必须设置）
            byte[] data = new byte[]{(byte) 0x11, (byte) 0x90};
            ByteBuffer csd_0 = ByteBuffer.wrap(data);
            mediaFormat.setByteBuffer("csd-0", csd_0);
            //解码器配置
            mDecoder.configure(mediaFormat, null, null, 0);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (mDecoder == null) {
            return false;
        }
        mDecoder.start();
        return true;
    }

    /**
     * aac解码+播放
     */
    public void decode(byte[] buf, int offset, int length) {
        //输入ByteBuffer
        ByteBuffer[] codecInputBuffers = mDecoder.getInputBuffers();
        //输出ByteBuffer
        ByteBuffer[] codecOutputBuffers = mDecoder.getOutputBuffers();
        //等待时间，0->不等待，-1->一直等待
        long kTimeOutUs = 0;
        try {
            //返回一个包含有效数据的input buffer的index,-1->不存在
            int inputBufIndex = mDecoder.dequeueInputBuffer(kTimeOutUs);
            if (inputBufIndex >= 0) {
                //获取当前的ByteBuffer
                ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                //清空ByteBuffer
                dstBuf.clear();
                //填充数据
                dstBuf.put(buf, offset, length);
                //将指定index的input buffer提交给解码器
                mDecoder.queueInputBuffer(inputBufIndex, 0, length, 0, 0);
            }
            //编解码器缓冲区
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            //返回一个output buffer的index，-1->不存在
            int outputBufferIndex = mDecoder.dequeueOutputBuffer(info, kTimeOutUs);

            if (outputBufferIndex < 0) {
                //记录解码失败的次数
                count++;
            }
            ByteBuffer outputBuffer;
            while (outputBufferIndex >= 0) {
                //获取解码后的ByteBuffer
                outputBuffer = codecOutputBuffers[outputBufferIndex];
                //用来保存解码后的数据
                byte[] outData = new byte[info.size];

                outputBuffer.get(outData);
                short[] audioData =BytesTransUtils.getInstance().Bytes2Shorts(outData);
                outputBuffer.clear();
                //释放已经解码的buffer
                mDecoder.releaseOutputBuffer(outputBufferIndex, false);
                //解码未解完的数据
                outputBufferIndex = mDecoder.dequeueOutputBuffer(info, kTimeOutUs);
                bufferSlice.input(audioData, audioData.length, (int)Ticker.Instance().elapsedTime(), audioData.length * 1000/ KEY_SAMPLE_RATE, new BufferSlice.ISliceOutput() {
                    @Override
                    public void onOutput(short[] slice, int stamp) {

                         slice = ns.process(slice,PCM_SLICE_MS);
                        WebRtcJni.WebRtcAgc.ResultOfProcess ret = agc.process(slice,slice.length,0,0);
                        slice = ret.out;
                       // slice = BytesTransUtils.getInstance().Shorts2Bytes(slice);
                        //清空缓存
                        byte[] ad= BytesTransUtils.getInstance().Shorts2Bytes(slice);
                        //播放解码后的数据
                        mPlayer.playAudioTrack(ad, 0,ad.length );


//                        byte [] encode_data = faac.encode(slice);
//                        if(encode_data != null){
//                            try {
//                                file.write(encode_data);
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//
//                        //bufferSlice内部的切片缓存(slice)是复用的，所以需要拷贝出来防止覆盖
//                        short[] slice_copy = new short[slice.length];
//
//                        System.arraycopy(slice,0,slice_copy,0,slice.length);
//
//                        pcmDataArr.add(slice_copy);
//                        final boolean vad_status = vad.process(SAMPLE_RATE,slice_copy,false);
//                        handler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                lb_vad_status.setText(vad_status ? "有声":"无声");
//                            }
//                        });
                    }
                });

                //outData1=ns.process(outData1,20);

            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    //返回解码失败的次数
    public int getCount() {
        return count;
    }

    /**
     * 释放资源
     */
    public void stop() {
        try {
            if (mPlayer != null) {
                mPlayer.release();
                mPlayer = null;
            }
            if (mDecoder != null) {
                mDecoder.stop();
                mDecoder.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}