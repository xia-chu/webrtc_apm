package com.google.webrtc.apm;

public class Faac {
    static {
        System.loadLibrary("faac");
    }

    /**
     * 构建aac编码器
     * @param sample_rate 采样率
     * @param channle 通道数
     * @param quality 质量,0~100
     */
    public Faac(int sample_rate,int channle,int quality){
        ctx = aac_encoder_create(sample_rate,channle,quality);
        if(ctx == 0){
            throw new IllegalArgumentException("aac_encoder_create failed");
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        release();
    }
    public void release(){
        if(ctx != 0){
            aac_encoder_destory(ctx);
            ctx = 0;
        }
    }

    /**
     * 开始编码数据
     * @param data 输入的数据
     * @return
     */
    public byte[] encode(short[] data) {
        return aac_encoder_input(ctx,data);
    }

    private long ctx = 0;

    private static native long aac_encoder_create(int sample_rate,int channle,int quality);
    private static native void aac_encoder_destory(long ctx);
    private static native byte[]  aac_encoder_input(long ctx, short[] data);

}
