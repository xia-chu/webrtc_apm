package com.google.webrtc.apm;

public class Faac {
    static {
        System.loadLibrary("faac");
    }

    public Faac(int sample_rate,int channle){
        ctx = aac_encoder_create(sample_rate,channle);
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
    public short[] encode(short[] data) {
        return aac_encoder_input(ctx,data);
    }

    private long ctx = 0;

    private static native long aac_encoder_create(int sample_rate,int channle);
    private static native void aac_encoder_destory(long ctx);
    private static native short[]  aac_encoder_input(long ctx, short[] data);

}
