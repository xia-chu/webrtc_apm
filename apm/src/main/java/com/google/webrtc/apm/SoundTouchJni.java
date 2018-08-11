package com.google.webrtc.apm;

public class SoundTouchJni {
    static {
        System.loadLibrary("soundtouch");
    }

    public SoundTouchJni(){
        ctx = ijk_soundtouch_create();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        release();
    }
    public void release(){
        if(ctx != 0){
            ijk_soundtouch_destroy(ctx);
            ctx = 0;
        }
    }
    public int translate(byte[] data,
                         int offset,
                         int len,
                         float speed,
                         float pitch,
                         int bytes_per_sample,
                         int n_channel,
                         int n_sampleRate){
        return ijk_soundtouch_translate(ctx,data,offset,len,speed,pitch,bytes_per_sample,n_channel,n_sampleRate);
    }

    private long ctx = 0;

    private static native long ijk_soundtouch_create();
    private static native void ijk_soundtouch_destroy(long ctx);
    private static native int  ijk_soundtouch_translate(long ctx,
                                                        byte[] data,
                                                        int offset,
                                                        int len,
                                                        float speed,
                                                        float pitch,
                                                        int bytes_per_sample,
                                                        int n_channel,
                                                        int n_sampleRate);

}
