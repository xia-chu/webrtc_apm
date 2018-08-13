package com.google.webrtc.apm;

public class WebRtcJni {
    static {
        System.loadLibrary("webrtc_apm");
    }

    /**
     * 静音检测
     */
    public static class  WebRtcVad
    {
        /**
         * 构建VAD对象
         * @param mode 激进程度(0, 1, 2, or 3).
         */
        public WebRtcVad(int mode){
            ctx = WebRtcVad_Create();
            if(ctx == 0){
                throw new RuntimeException("WebRtcVad_Create failed");
            }
            WebRtcVad_Init(ctx);
            WebRtcVad_set_mode(ctx,mode);
        }

        /**
         * 立即释放JNI底层对象
         */
        public void release(){
            if(ctx != 0 ){
                WebRtcVad_Free(ctx);
                ctx = 0;
            }
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            release();
        }

        /**
         * 判断是否有声音
         * @param fs 采样率
         * @param audio_frame 音频数据
         * @return 如果有声音就返回true
         */
        public boolean process(int fs,short[] audio_frame){
            return process(fs,audio_frame,true);
        }

        /**
         * 判断是否有声音
         * @param fs 采样率(Hz: 8000, 16000, or 32000)
         * @param audio_frame 音频数据
         * @param delay 是否延迟检测，如果启用在，则连续3秒静音才会判定静音
         * @return 如果有声音就返回true
         */
        public boolean process(int fs,short[] audio_frame ,boolean delay){
            if(1 != WebRtcVad_Process(ctx,fs,audio_frame)){
                //无声音，如果静音计时超过3秒,则认为真的无声音
                return delay ? ticker.elapsedTime() < 3 * 1000 : false;
            }

            //有声音,重置静音计时器
            ticker.resetTime();
            return true;
        }
        private long ctx = 0;
        private Ticker ticker = new Ticker();
    }


    // Creates an instance to the VAD structure.
    //
    // returns      : 0 - (error), other - (vad instance handle)
    private static native long WebRtcVad_Create();

    // Frees the dynamic memory of a specified VAD instance.
    //
    // - handle [i] : Pointer to VAD instance that should be freed.
    //
    // returns      : 0 - (OK), -1 - (NULL pointer in)
    private static native int WebRtcVad_Free(long handle);

    // Initializes a VAD instance.
    //
    // - handle [i/o] : Instance that should be initialized.
    //
    // returns        : 0 - (OK),
    //                 -1 - (NULL pointer or Default mode could not be set).
    private static native int WebRtcVad_Init(long handle);

    // Sets the VAD operating mode. A more aggressive (higher mode) VAD is more
    // restrictive in reporting speech. Put in other words the probability of being
    // speech when the VAD returns 1 is increased with increasing mode. As a
    // consequence also the missed detection rate goes up.
    //
    // - handle [i/o] : VAD instance.
    // - mode   [i]   : Aggressiveness mode (0, 1, 2, or 3).
    //
    // returns        : 0 - (OK),
    //                 -1 - (NULL pointer, mode could not be set or the VAD instance
    //                       has not been initialized).
    private static native int WebRtcVad_set_mode(long handle,int mode);

    // Calculates a VAD decision for the |audio_frame|. For valid sampling rates
    // frame lengths, see the description of WebRtcVad_ValidRatesAndFrameLengths().
    //
    // - handle       [i/o] : VAD Instance. Needs to be initialized by
    //                        WebRtcVad_Init() before call.
    // - fs           [i]   : Sampling frequency (Hz): 8000, 16000, or 32000
    // - audio_frame  [i]   : Audio frame buffer.
    //
    // returns              : 1 - (Active Voice),
    //                        0 - (Non-active Voice),
    //                       -1 - (Error)
    private static native int WebRtcVad_Process(long handle,int fs,short[] audio_frame);


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    /**
     * 低频噪音消除
     */
    public static class WebRtcNs{
        /**
         * 构建NS对象
         * @param fs 采样率
         * @param mode 模式(0: Mild, 1: Medium , 2: Aggressive)
         */
        public WebRtcNs(int fs,int mode){
            ctx = WebRtcNs_Create();
            if(ctx == 0){
                throw new RuntimeException("WebRtcNs_Create failed");
            }
            WebRtcNs_Init(ctx,fs);
            WebRtcNs_set_policy(ctx,mode);
        }

        /**
         * 立即释放JNI对象
         */
        public void release(){
            if(ctx != 0 ){
                WebRtcNs_Free(ctx);
                ctx = 0;
            }
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            release();
        }

        /**
         * 消除低频背景噪声
         * @param spframe 采集后需要消噪的声音
         * @param sampleMS 采样毫秒数，必须为10ms的整数倍
         * @return null出现错误，否则为消噪之后的数据
         */
        public short [] process(short[] spframe,int sampleMS){
            return WebRtcNs_Process(ctx,spframe,sampleMS);
        }

        private long ctx = 0;
    }

    /*
     * This function creates an instance to the noise suppression structure
     *
     * Return value         :  0 - Error
     *                        other  - (ns instance handle)
     */
    private static native long WebRtcNs_Create();

    /*
     * This function frees the dynamic memory of a specified noise suppression
     * instance.
     *
     * Input:
     *      - NS_inst       : Pointer to NS instance that should be freed
     *
     * Return value         :  0 - Ok
     *                        -1 - Error
     */
    private static native int WebRtcNs_Free(long NS_inst);

    /*
     * This function initializes a NS instance and has to be called before any other
     * processing is made.
     *
     * Input:
     *      - NS_inst       : Instance that should be initialized
     *      - fs            : sampling frequency
     * Return value         :  0 - Ok
     *                        -1 - Error
     */
    private static native int WebRtcNs_Init(long NS_inst,int fs);

    /*
     * This changes the aggressiveness of the noise suppression method.
     *
     * Input:
     *      - NS_inst       : Noise suppression instance.
     *      - mode          : 0: Mild, 1: Medium , 2: Aggressive
     *
     * Return value         :  0 - Ok
     *                        -1 - Error
     */
    private static native int WebRtcNs_set_policy(long NS_inst,int mode);


    /*
     * This functions does Noise Suppression for the inserted speech frame. The
     * input and output signals should always be 10ms (80 or 160 samples).
     *
     * Input
     *      - NS_inst       : Noise suppression instance.
     *      - spframe       : Pointer to speech frame buffer for L band
     *      - sampleMS      : 采样毫秒数，必须为10ms的整数倍
     *
     * Return value         :  null - Error
     *                        other - output frame
     */
    private static native short[] WebRtcNs_Process(long NS_inst,short[] spframe,int sampleMS);

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    /**
     * 消回声
     */
    public static class WebRtcAecm{
        /**
         * 构建AECM对象
         * @param fs 采样率
         * @param cngMode 是否生成舒适噪音
         * @param echoMode 消回声等级(0, 1, 2, 3 (default), 4)
         */
        public WebRtcAecm(int fs, boolean cngMode, int echoMode){
            ctx = WebRtcAecm_Create();
            if(ctx == 0){
                throw new RuntimeException("WebRtcAecm_Create failed");
            }
            WebRtcAecm_Init(ctx,fs);
            WebRtcAecm_set_config(ctx,cngMode ? 1 : 0,echoMode);
        }

        /**
         * 立即释放JNI底层对象
         */
        public void  release(){
            if(ctx != 0 ){
                WebRtcAecm_Free(ctx);
                ctx = 0;
            }
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            release();
        }

        /**
         * 输入录制到的数据，开始消除回声
         * @param nearendNoisy 近端数据(也就是MIC录制到的数据)
         * @param nearendClean 近端去除噪音后的数据，可以为null
         * @param nrOfSamples 进度数据采样数，16bit单声道采样数等于nearendNoisy.length,必须为80或160个采样
         * @param msInSndCardBuf 系统延时,等于采集延时+播放延时+播放列队缓存数据长度
         * @return null:出现错误，否则为消回声后的数据
         */
        public synchronized short [] process(short[] nearendNoisy,short[] nearendClean,int nrOfSamples, int msInSndCardBuf){
            return WebRtcAecm_Process(ctx,nearendNoisy,nearendClean,nrOfSamples,msInSndCardBuf);
        }

        /**
         * 输入远端数据
         * @param farend 远端数据(也就是收到对方语音需要播放的数据)
         * @param nrOfSamples 远端数据采样数，16bit单声道采样数等于farend.length
         * @return 0: OK , -1: error
         */
        public synchronized int bufferFarend(short[] farend,int nrOfSamples){
            return WebRtcAecm_BufferFarend(ctx,farend,nrOfSamples);
        }

        /**
         * 输入远端数据(输入byte[] 版本)
         * @param farend 远端数据(也就是收到对方语音需要播放的数据)
         * @param nrOfSamples 远端数据采样数，16bit单声道采样数等于farend.length/2
         * @return 0: OK , -1: error
         */
        public synchronized int bufferFarendBytes(byte[] farend,int nrOfSamples){
            return WebRtcAecm_BufferFarendBytes(ctx,farend,nrOfSamples);
        }
        private long ctx = 0;
    }

    /*
     * Allocates the memory needed by the AECM. The memory needs to be
     * initialized separately using the WebRtcAecm_Init() function.
     *
     * int32_t return                   0: error
     *                                 other: aecm instance handle
     */
    private static native long WebRtcAecm_Create();


    /*
     * This function releases the memory allocated by WebRtcAecm_Create()
     *
     * Inputs                       Description
     * -------------------------------------------------------------------
     * void *aecmInst               Pointer to the AECM instance
     *
     * Outputs                      Description
     * -------------------------------------------------------------------
     * int32_t  return        0: OK
     *                             -1: error
     */
    private static native int WebRtcAecm_Free(long aecmInst);


    /*
     * Initializes an AECM instance.
     *
     * Inputs                       Description
     * -------------------------------------------------------------------
     * void           *aecmInst     Pointer to the AECM instance
     * int32_t        sampFreq      Sampling frequency of data
     *
     * Outputs                      Description
     * -------------------------------------------------------------------
     * int32_t        return        0: OK
     *                             -1: error
     */
    private static native int WebRtcAecm_Init(long aecmInst,int sampFreq);


    /*
     * This function enables the user to set certain parameters on-the-fly
     *
     * Inputs                       Description
     * -------------------------------------------------------------------
     * void     *aecmInst           Pointer to the AECM instance
     * int16_t cngMode;            // AECM_FALSE, AECM_TRUE (default)
     * int16_t echoMode;           // 0, 1, 2, 3 (default), 4
     *
     * Outputs                      Description
     * -------------------------------------------------------------------
     * int32_t        return        0: OK
     *                             -1: error
     */
    private static native int WebRtcAecm_set_config(long ctx,int cngMode,int echoMode);


    /*
     * Inserts an 80 or 160 sample block of data into the farend buffer.
     *
     * Inputs                       Description
     * -------------------------------------------------------------------
     * void           *aecmInst     Pointer to the AECM instance
     * int16_t        *farend       In buffer containing one frame of
     *                              farend signal
     * int16_t        nrOfSamples   Number of samples in farend buffer
     *
     * Outputs                      Description
     * -------------------------------------------------------------------
     * int32_t        return        0: OK
     *                             -1: error
     */
    private static native int WebRtcAecm_BufferFarend(long aecmInst,short[] farend,int nrOfSamples);
    private static native int WebRtcAecm_BufferFarendBytes(long aecmInst,byte[] farend,int nrOfSamples);

    /*
     * Runs the AECM on an 80 or 160 sample blocks of data.
     *
     * Inputs                       Description
     * -------------------------------------------------------------------
     * void           *aecmInst      Pointer to the AECM instance
     * int16_t        *nearendNoisy  In buffer containing one frame of
     *                               reference nearend+echo signal. If
     *                               noise reduction is active, provide
     *                               the noisy signal here.
     * int16_t        *nearendClean  In buffer containing one frame of
     *                               nearend+echo signal. If noise
     *                               reduction is active, provide the
     *                               clean signal here. Otherwise pass a
     *                               NULL pointer.
     * int16_t        nrOfSamples    Number of samples in nearend buffer
     * int16_t        msInSndCardBuf Delay estimate for sound card and
     *                               system buffers
     *
     * Outputs                      Description
     * -------------------------------------------------------------------
     * int32_t        return       null: error
     *                             other: Out buffer, one frame of processed nearend
     */
    private static native short[] WebRtcAecm_Process(long ctx,
                                                     short[] nearendNoisy,
                                                     short[] nearendClean,
                                                     int nrOfSamples,
                                                     int msInSndCardBuf);


    /**
     * 自动增益
     */
    public static class WebRtcAgc
    {
        /**
         * 构建AGC对象
         * @param minLevel Minimum possible mic level
         * @param maxLevel Maximum possible mic level
         * @param agcMode : 0 - Unchanged
         *                : 1 - Adaptive Analog Automatic Gain Control -3dBOv
         *                : 2 - Adaptive Digital Automatic Gain Control -3dBOv
         *                : 3 - Fixed Digital Gain 0dB
         * @param fs Sampling frequency
         */
        public WebRtcAgc(int minLevel,
                         int maxLevel,
                         int agcMode,
                         int fs){
            ctx = WebRtcAgc_Create();
            if(ctx == 0){
                throw new RuntimeException("WebRtcAgc_Create failed");
            }
            WebRtcAgc_Init(ctx,minLevel,maxLevel,agcMode,fs);
        }

        /**
         * 释放JNI底层对象
         */
        public void release(){
            if(ctx != 0){
                WebRtcAgc_Free(ctx);
                ctx = 0;
            }
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            release();
        }

        /**
         * 配置AGC
         * @param targetLevelDbfs default 3 (-3 dBOv), dbfs表示相对于full scale的下降值，0表示full scale，越小声音越大
         * @param compressionGaindB default 9 dB,在Fixed模式下，越大声音越大
         * @param limiterEnable default true (on)
         * @return
         */
        public int setConfig(int targetLevelDbfs,
                             int compressionGaindB,
                             boolean limiterEnable){
            return WebRtcAgc_set_config(ctx,targetLevelDbfs,compressionGaindB,limiterEnable ? 1:0);
        }

        /**
         * 输入远端数据
         * This function processes a 10/20ms frame of far-end speech to determine
         * if there is active speech. Far-end speech length can be either 10ms or
         * 20ms. The length of the input speech vector must be given in samples
         * (80/160 when FS=8000, and 160/320 when FS=16000 or FS=32000).
         *
         * @param inFar Far-end input speech vector (10 or 20ms)
         * @param samples Number of samples in input vector
         * @return 0:success ,-1 error
         */
        public int addFarend(short[] inFar,int samples){
            return WebRtcAgc_AddFarend(ctx,inFar,samples);
        }

        /**
         * This function processes a 10/20ms frame of microphone speech to determine
         * if there is active speech. Microphone speech length can be either 10ms or
         * 20ms. The length of the input speech vector must be given in samples
         * (80/160 when FS=8000, and 160/320 when FS=16000 or FS=32000). For very low
         * input levels, the input signal is increased in level by multiplying and
         * overwriting the samples in inMic[].
         *
         * This function should be called before any further processing of the
         * near-end microphone signal.
         *
         * @param inMic Microphone input speech vector (10 or 20 ms)
         * @param samples Number of samples in input vector
         * @return 0:success , -1 :error
         */
        public int addMic(short[] inMic,int samples){
            return WebRtcAgc_AddMic(ctx,inMic,samples);
        }


        /**
         * virtualMic方法返回值对象
         */
        public static class ResultOfVirtualMic
        {
            /**
             * 函数执行返回值,0:success,-1:error
             */
            public int ret;

            /**
             * Adjusted microphone level after processing
             */
            public int micLevelOut;
        }

        /**
         * This function replaces the analog microphone with a virtual one.
         * It is a digital gain applied to the input signal and is used in the
         * agcAdaptiveDigital mode where no microphone level is adjustable.
         * Microphone speech length can be either 10ms or 20ms. The length of the
         * input speech vector must be given in samples (80/160 when FS=8000, and
         * 160/320 when FS=16000 or FS=32000).
         *
         * @param inMic Microphone input speech vector for (10 or 20 ms),and Microphone output after processing
         * @param samples Number of samples in input vector
         * @param micLevelIn Input level of microphone (static)
         * @return 结果对象
         * @see ResultOfVirtualMic
         */
        public ResultOfVirtualMic virtualMic(short[] inMic,
                                             int samples,
                                             int micLevelIn){
            ResultOfVirtualMic obj = new  ResultOfVirtualMic();
            int[] micLevelOutArr = new int[1];
            obj.ret = WebRtcAgc_VirtualMic(ctx,inMic,samples,micLevelIn,micLevelOutArr);
            obj.micLevelOut = micLevelOutArr[0];
            return obj;
        }


        /**
         * process方法返回值对象
         */
        public static class ResultOfProcess
        {
            /**
             * 函数执行返回值,0:success,-1:error
             */
            public int ret;

            /**
             * 增益处理后的数据
             */
            public short[] out;

            /**
             *  Adjusted microphone volume level
             */
            public int outMicLevel;

            /**
             * A returned value of 1 indicates a saturation event
             * has occurred and the volume cannot be further
             * reduced. Otherwise will be set to 0.
             */
            public int saturationWarning;
        }


        /**
         * This function processes a 10/20ms frame and adjusts (normalizes) the gain
         * both analog and digitally. The gain adjustments are done only during
         * active periods of speech. The input speech length can be either 10ms or
         * 20ms and the output is of the same length. The length of the speech
         * vectors must be given in samples (80/160 when FS=8000, and 160/320 when
         * FS=16000 or FS=32000). The echo parameter can be used to ensure the AGC will
         * not adjust upward in the presence of echo.
         *
         * This function should be called after processing the near-end microphone
         * signal, in any case after any echo cancellation.
         *
         *
         * @param inNear Near-end input speech vector (10 or 20 ms)
         * @param samples Number of samples in input/output vector
         * @param inMicLevel Current microphone volume level
         * @param echo Set to 0 if the signal passed to add_mic is
         *             almost certainly free of echo; otherwise set
         *             to 1. If you have no information regarding echo
         *             set to 0.
         * @return 返回值对象
         * @see ResultOfProcess
         */
        public ResultOfProcess process(short[] inNear,
                                       int samples,
                                       int inMicLevel,
                                       int echo){
            ResultOfProcess obj = new ResultOfProcess();
            obj.out = new short[inNear.length];

            int[] outMicLevelArr = new int[1];
            int[] saturationWarningArr = new int[1];
            obj.ret = WebRtcAgc_Process(ctx,inNear,samples,obj.out,inMicLevel,outMicLevelArr,echo,saturationWarningArr);
            obj.outMicLevel = outMicLevelArr[0];
            obj.saturationWarning = saturationWarningArr[0];
            return obj;
        }

        private long ctx = 0;
    }


    /*
     * This function processes a 10/20ms frame of far-end speech to determine
     * if there is active speech. Far-end speech length can be either 10ms or
     * 20ms. The length of the input speech vector must be given in samples
     * (80/160 when FS=8000, and 160/320 when FS=16000 or FS=32000).
     *
     * Input:
     *      - agcInst           : AGC instance.
     *      - inFar             : Far-end input speech vector (10 or 20ms)
     *      - samples           : Number of samples in input vector
     *
     * Return value:
     *                          :  0 - Normal operation.
     *                          : -1 - Error
     */
    private static native int WebRtcAgc_AddFarend(long agcInst,
                                                  short[] inFar,
                                                  int samples);




    /*
     * This function processes a 10/20ms frame of microphone speech to determine
     * if there is active speech. Microphone speech length can be either 10ms or
     * 20ms. The length of the input speech vector must be given in samples
     * (80/160 when FS=8000, and 160/320 when FS=16000 or FS=32000). For very low
     * input levels, the input signal is increased in level by multiplying and
     * overwriting the samples in inMic[].
     *
     * This function should be called before any further processing of the
     * near-end microphone signal.
     *
     * Input:
     *      - agcInst           : AGC instance.
     *      - inMic             : Microphone input speech vector (10 or 20 ms) for
     *                            L band
     *      - samples           : Number of samples in input vector
     *
     * Return value:
     *                          :  0 - Normal operation.
     *                          : -1 - Error
     */
    private static native int WebRtcAgc_AddMic(long agcInst,
                                               short [] inMic,
                                               int samples);



    /*
     * This function replaces the analog microphone with a virtual one.
     * It is a digital gain applied to the input signal and is used in the
     * agcAdaptiveDigital mode where no microphone level is adjustable.
     * Microphone speech length can be either 10ms or 20ms. The length of the
     * input speech vector must be given in samples (80/160 when FS=8000, and
     * 160/320 when FS=16000 or FS=32000).
     *
     * Input:
     *      - agcInst           : AGC instance.
     *      - inMic             : Microphone input speech vector for (10 or 20 ms)
     *                            L band
     *      - samples           : Number of samples in input vector
     *      - micLevelIn        : Input level of microphone (static)
     *
     * Output:
     *      - inMic             : Microphone output after processing (L band)
     *      - micLevelOut       : Adjusted microphone level after processing
     *
     * Return value:
     *                          :  0 - Normal operation.
     *                          : -1 - Error
     */
    private static native  int WebRtcAgc_VirtualMic(long agcInst,
                                                    short[] inMic,
                                                    int samples,
                                                    int micLevelIn,
                                                    int[] micLevelOut);


    /*
     * This function processes a 10/20ms frame and adjusts (normalizes) the gain
     * both analog and digitally. The gain adjustments are done only during
     * active periods of speech. The input speech length can be either 10ms or
     * 20ms and the output is of the same length. The length of the speech
     * vectors must be given in samples (80/160 when FS=8000, and 160/320 when
     * FS=16000 or FS=32000). The echo parameter can be used to ensure the AGC will
     * not adjust upward in the presence of echo.
     *
     * This function should be called after processing the near-end microphone
     * signal, in any case after any echo cancellation.
     *
     * Input:
     *      - agcInst           : AGC instance
     *      - inNear            : Near-end input speech vector (10 or 20 ms) for
     *                            L band
     *      - samples           : Number of samples in input/output vector
     *      - inMicLevel        : Current microphone volume level
     *      - echo              : Set to 0 if the signal passed to add_mic is
     *                            almost certainly free of echo; otherwise set
     *                            to 1. If you have no information regarding echo
     *                            set to 0.
     *
     * Output:
     *      - outMicLevel       : Adjusted microphone volume level
     *      - out               : Gain-adjusted near-end speech vector (L band)
     *                          : May be the same vector as the input.
     *      - saturationWarning : A returned value of 1 indicates a saturation event
     *                            has occurred and the volume cannot be further
     *                            reduced. Otherwise will be set to 0.
     *
     * Return value:
     *                          :  0 - Normal operation.
     *                          : -1 - Error
     */
    private static native int WebRtcAgc_Process(long agcInst,
                                                short[] inNear,
                                                int samples,
                                                short[] out,
                                                int inMicLevel,
                                                int[] outMicLevel,
                                                int echo,
                                                int[] saturationWarning);

    /*
     * This function sets the config parameters (targetLevelDbfs,
     * compressionGaindB and limiterEnable).
     *
     * Input:
     *      - agcInst           : AGC instance
     *
     *      - targetLevelDbfs;   // default 3 (-3 dBOv)
     *      - compressionGaindB; // default 9 dB
     *      - limiterEnable;     // default kAgcTrue (on)
     * Output:
     *
     * Return value:
     *                          :  0 - Normal operation.
     *                          : -1 - Error
     */
    private static native int WebRtcAgc_set_config(long agcInst,
                                                   int targetLevelDbfs,
                                                   int compressionGaindB,
                                                   int limiterEnable);



    /*
     * This function creates an AGC instance, which will contain the state
     * information for one (duplex) channel.
     *
     * Return value             : AGC instance if successful
     */
    private static native long WebRtcAgc_Create();


    /*
     * This function frees the AGC instance created at the beginning.
     *
     * Input:
     *      - agcInst           : AGC instance.
     *
     * Return value             :  0 - Ok
     *                            -1 - Error
     */
    private static native int WebRtcAgc_Free(long agcInst);


    /*
     * This function initializes an AGC instance.
     *
     * Input:
     *      - agcInst           : AGC instance.
     *      - minLevel          : Minimum possible mic level
     *      - maxLevel          : Maximum possible mic level
     *      - agcMode           : 0 - Unchanged
     *                          : 1 - Adaptive Analog Automatic Gain Control -3dBOv
     *                          : 2 - Adaptive Digital Automatic Gain Control -3dBOv
     *                          : 3 - Fixed Digital Gain 0dB
     *      - fs                : Sampling frequency
     *
     * Return value             :  0 - Ok
     *                            -1 - Error
     */
    private static native int WebRtcAgc_Init(long agcInst,
                                             int minLevel,
                                             int maxLevel,
                                             int agcMode,
                                             int fs);
}
