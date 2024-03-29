package com.google.webrtc.apmdemo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.webrtc.apm.Faac;
import com.google.webrtc.apm.Ticker;
import com.google.webrtc.apm.WebRtcJni.WebRtcVad;
import com.google.webrtc.apm.WebRtcJni.WebRtcNs;
import com.google.webrtc.apm.WebRtcJni.WebRtcAecm;
import com.google.webrtc.apm.WebRtcJni.WebRtcAgc;
import com.google.webrtc.apmdemo.file.ReadAACFileThread;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements AudioCapturer.OnAudioCapturedListener{

    Switch sw_record;
    TextView lb_vad_status,filePathtextView;
    Button bt_origin,bt_ns,bt_agc,bt_ns_agc,bt_agc_ns;

    AudioCapturer audioCapturer = new AudioCapturer();
    AudioPlayer audioPlayer = new AudioPlayer();
    private String path=null;
    final static int PCM_SLICE_MS = 20;
    final static int SAMPLE_RATE = 16000;

    WebRtcVad vad = new WebRtcVad(2);
    WebRtcNs ns = new WebRtcNs(SAMPLE_RATE,2);
    WebRtcAecm aecm = new WebRtcAecm(SAMPLE_RATE,false,3);
    WebRtcAgc agc = new WebRtcAgc(0,255,2,SAMPLE_RATE);

    Handler handler = new Handler();
    ArrayList<short[]> pcmDataArr = new ArrayList<>();
    TaskQuenu taskQuenu = new TaskQuenu();

    //每个切片20ms的pcm数据
    BufferSlice bufferSlice = new BufferSlice(SAMPLE_RATE * PCM_SLICE_MS / 1000);
    boolean interrupted = false;
    //MP3lame mp3;
    Faac faac;
    FileOutputStream file;

    ReadAACFileThread readAACFileThread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sw_record = findViewById(R.id.sw_record);
        lb_vad_status = findViewById(R.id.lb_vad_status);
        filePathtextView = findViewById(R.id.filePathtextView);
        bt_agc = findViewById(R.id.bt_agc);
        bt_agc_ns = findViewById(R.id.bt_agc_ns);
        bt_ns_agc = findViewById(R.id.bt_ns_agc);

        bt_ns = findViewById(R.id.bt_ns);
        bt_origin = findViewById(R.id.bt_origin);
        agc.setConfig(3,20,true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{"android.permission.RECORD_AUDIO", "android.permission.WRITE_EXTERNAL_STORAGE"},10);
        }
    }

    public void onClick_record(View view)  throws Exception{
        if (sw_record.isChecked()){
            pcmDataArr.clear();
            bufferSlice.clear();
            audioCapturer.setOnAudioCapturedListener(this);
            try {
                File path=new File( this.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() , "1.aac");
                this.path=path.getAbsolutePath();
                if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                    if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(MainActivity.this,"请开启存储权限",Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                if(!path.exists())path.createNewFile();
                file =  new FileOutputStream(path.getAbsolutePath());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            faac = new Faac(SAMPLE_RATE,1,100);
            //mp3 = new MP3lame(SAMPLE_RATE,1,SAMPLE_RATE,128,0);
            audioCapturer.startCapture();
        }else {
            audioCapturer.stopCapture();
            if(faac != null){
                try {
                    filePathtextView.setText(path);

                    //file.write(faac.encode());
                    faac.release();
                    faac = null;
                    file.flush();
                    file.close();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }
    @Override
    public void onAudioCaptured(short[] audioData, int stamp) {

        //audioData = ns.process(audioData,PCM_SLICE_MS);
//        WebRtcAgc.ResultOfProcess ret = agc.process(audioData,audioData.length,micLevelIn,0);
//        if (ret.ret != 0){
//            Log.e("TAG","agc.process faield!");
//            //return pcm;
//        }
//        if (ret.saturationWarning == 1){
//            Log.e("TAG","agc.process saturationWarning == 1");
//        }
        //micLevelIn = ret.outMicLevel;
        //audioData= ret.out;



        bufferSlice.input(audioData, audioData.length, stamp, audioData.length * 1000/ SAMPLE_RATE, new BufferSlice.ISliceOutput() {
            @Override
            public void onOutput(short[] slice, int stamp) {
               // slice = ns.process(slice,PCM_SLICE_MS);
                byte [] encode_data = faac.encode(slice);
                if(encode_data != null){
                    try {
                        file.write(encode_data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                //bufferSlice内部的切片缓存(slice)是复用的，所以需要拷贝出来防止覆盖
                short[] slice_copy = new short[slice.length];

                System.arraycopy(slice,0,slice_copy,0,slice.length);

                pcmDataArr.add(slice_copy);
                final boolean vad_status = vad.process(SAMPLE_RATE,slice_copy,false);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        lb_vad_status.setText(vad_status ? "有声":"无声");
                    }
                });
            }
        });

    }


    public void onClick_originPlay(View view) {
        playAudio(new IBerforePlayAudio() {
            @Override
            public short[] onBerforePlayAudio(short[] pcm) {
                return pcm;
            }
        });
    }

    public void onClick_nsPlaye(View view) {
        playAudio(new IBerforePlayAudio() {
            @Override
            public short[] onBerforePlayAudio(short[] pcm) {
                return  ns.process(pcm,PCM_SLICE_MS);
            }
        });
    }

    int micLevelIn = 0;
    public void onClick_agcPlay(View view) {
        playAudio(new IBerforePlayAudio() {
            @Override
            public short[] onBerforePlayAudio(short[] pcm) {
                WebRtcAgc.ResultOfProcess ret = agc.process(pcm,pcm.length,micLevelIn,0);
                if (ret.ret != 0){
                    Log.e("TAG","agc.process faield!");
                    return pcm;
                }
                if (ret.saturationWarning == 1){
                    Log.e("TAG","agc.process saturationWarning == 1");
                }
                micLevelIn = ret.outMicLevel;
                return ret.out;
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode,  Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {


            Uri uri = data.getData();

            String pathString = UriUtil.getPath(this, uri);

            filePathtextView.setText(pathString);
            path=pathString;


                /*
                 //保存读取到的内容
            StringBuilder result = new StringBuilder();

                //获取输入流
                InputStream is = this.getContentResolver().openInputStream(uri);
                //创建用于字符输入流中读取文本的bufferReader对象
                BufferedReader br = new BufferedReader(new InputStreamReader(is));



                String line;
                while ((line = br.readLine()) != null) {
                    //将读取到的内容放入结果字符串
                    result.append(line);
                }
                //文件中的内容
                String content = result.toString();


                 */


        }
    }



    public void onClick_agcNSPlay(View view) {
        playAudio(new IBerforePlayAudio() {
            @Override
            public short[] onBerforePlayAudio(short[] pcm) {
                WebRtcAgc.ResultOfProcess ret = agc.process(pcm,pcm.length,micLevelIn,0);
                if (ret.ret != 0){
                    Log.e("TAG","agc.process faield!");
                    return pcm;
                }
                if (ret.saturationWarning == 1){
                    Log.e("TAG","agc.process saturationWarning == 1");
                }
                micLevelIn = ret.outMicLevel;
                return ns.process(ret.out,PCM_SLICE_MS);
            }
        });
    }

    public void onClick_nsAgcPlay(View view) {
        playAudio(new IBerforePlayAudio() {
            @Override
            public short[] onBerforePlayAudio(short[] pcm) {
                pcm = ns.process(pcm,PCM_SLICE_MS);
                WebRtcAgc.ResultOfProcess ret = agc.process(pcm,pcm.length,micLevelIn,0);
                if (ret.ret != 0){
                    Log.e("TAG","agc.process faield!");
                    return pcm;
                }
                if (ret.saturationWarning == 1){
                    Log.e("TAG","agc.process saturationWarning == 1");
                }
                micLevelIn = ret.outMicLevel;
                return ret.out;
            }
        });
    }


    public void onClick_stopnsAgcPlay1(View view){
        readAACFileThread.setFinish(true);
    }

    public void onClick_nsAgcPlay1(View view) {

        readAACFileThread = new ReadAACFileThread(path);
        readAACFileThread.start();

//        playAudio(new IBerforePlayAudio() {
//            @Override
//            public short[] onBerforePlayAudio(short[] pcm) {
//                pcm = ns.process(pcm,PCM_SLICE_MS);
//                WebRtcAgc.ResultOfProcess ret = agc.process(pcm,pcm.length,micLevelIn,0);
//                if (ret.ret != 0){
//                    Log.e("TAG","agc.process faield!");
//                    return pcm;
//                }
//                if (ret.saturationWarning == 1){
//                    Log.e("TAG","agc.process saturationWarning == 1");
//                }
//                micLevelIn = ret.outMicLevel;
//                return ret.out;
//            }
//        });
    }

    public void onClick_choosefile(View view) {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //intent.type = "*/*"       //   /*/ 此处是任意类型任意后缀
        intent.setType("audio/*"); //选择音频

        //intent.setType(“video/*”) //选择视频 （mp4 3gp 是android支持的视频格式）

        //intent.setType(“video/*;image/*”)//同时选择视频和图片
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 100);
//
//        playAudio(new IBerforePlayAudio() {
//            @Override
//            public short[] onBerforePlayAudio(short[] pcm) {
//                pcm = ns.process(pcm,PCM_SLICE_MS);
//                WebRtcAgc.ResultOfProcess ret = agc.process(pcm,pcm.length,micLevelIn,0);
//                if (ret.ret != 0){
//                    Log.e("TAG","agc.process faield!");
//                    return pcm;
//                }
//                if (ret.saturationWarning == 1){
//                    Log.e("TAG","agc.process saturationWarning == 1");
//                }
//                micLevelIn = ret.outMicLevel;
//                return ret.out;
//            }
//        });
    }

    public void onClick_rec_play(View view) {
        setEnable(false);
        audioCapturer.setOnAudioCapturedListener(new AudioCapturer.OnAudioCapturedListener() {
            @Override
            public void onAudioCaptured(short[] audioData, int stamp) {
                bufferSlice.input(audioData, audioData.length, stamp, audioData.length * 1000 / SAMPLE_RATE, new BufferSlice.ISliceOutput() {
                    @Override
                    public void onOutput(short[] slice, int stamp) {
                        final short [] nearendNoisy = new short[slice.length];
                        System.arraycopy(slice,0,nearendNoisy,0,slice.length);

                        taskQuenu.async(new TaskQuenu.Task() {
                            @Override
                            public void run() throws TaskQuenu.ExitInterruptedException {
                                short[] nearendClean = ns.process(nearendNoisy,PCM_SLICE_MS);
                                short[] aecm_out = aecm.process(nearendNoisy,nearendClean,nearendNoisy.length, audioCapturer.getRecordDelayMS() + audioPlayer.getPlayDelayMS());
                                if(aecm_out == null){
                                    aecm_out = nearendClean;
                                    Log.e("TAG","aecm.process return null");
                                }
                                aecm.bufferFarend(aecm_out,aecm_out.length);
                                audioPlayer.play(aecm_out,0,aecm_out.length);
                            }
                        });
                    }
                });
            }
        });
        audioCapturer.startCapture();
        audioPlayer.startPlayer();
    }


    private void playAudio(final IBerforePlayAudio cb){
        sw_record.setChecked(false);
        audioCapturer.stopCapture();
        taskQuenu.async(new TaskQuenu.Task() {
            @Override
            public void run() throws TaskQuenu.ExitInterruptedException {
                setEnable(false);
                audioPlayer.startPlayer();
                interrupted = false;
                micLevelIn = 0;
                for (short[] pcm : pcmDataArr){
                    short[] pcm_after = cb.onBerforePlayAudio(pcm);
                    if(pcm_after != null){
                        audioPlayer.play(pcm_after,0,pcm_after.length);
                    }
                    if (interrupted){
                        break;
                    }
                }
                audioPlayer.stopPlayer();
                setEnable(true);
            }
        });
    }



    private interface IBerforePlayAudio
    {
       short[] onBerforePlayAudio(short[] pcm);
    }

    private void setEnable(final boolean enable){
        handler.post(new Runnable() {
            @Override
            public void run() {
                bt_agc.setEnabled(enable);
                bt_agc_ns.setEnabled(enable);
                bt_ns_agc.setEnabled(enable);
                bt_ns.setEnabled(enable);
                bt_origin.setEnabled(enable);
                sw_record.setEnabled(enable);
            }
        });
    }

    Ticker backTicker = new Ticker();
    @Override
    public void onBackPressed() {
        if(backTicker.elapsedTime() > 2*1000){
            Toast.makeText(this,"两秒内连续点击返回键退出程序", Toast.LENGTH_LONG).show();
            backTicker.resetTime();
        }else {
            super.onBackPressed();
        }
        interrupted = true;
    }
}
