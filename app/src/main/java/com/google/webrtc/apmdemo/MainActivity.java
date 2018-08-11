package com.google.webrtc.apmdemo;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.google.webrtc.apm.WebRtcJni.WebRtcVad;
import com.google.webrtc.apm.WebRtcJni.WebRtcNs;
import com.google.webrtc.apm.WebRtcJni.WebRtcAecm;
import com.google.webrtc.apm.WebRtcJni.WebRtcAgc;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements AudioCapturer.OnAudioCapturedListener{

    Switch sw_record;
    AudioCapturer audioCapturer = new AudioCapturer();
    AudioPlayer audioPlayer = new AudioPlayer();
    ArrayList<short[]> pcmDataArr = new ArrayList<>();
    TextView lb_vad_status;
    WebRtcVad vad = new WebRtcVad(2);
    WebRtcNs ns = new WebRtcNs(16000,1);
    WebRtcAecm aecm = new WebRtcAecm(16000,false,3);
    WebRtcAgc agc = new WebRtcAgc(0,255,2,16000);
    Handler handler = new Handler();
    TaskQuenu taskQuenu = new TaskQuenu();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sw_record = findViewById(R.id.sw_record);
        lb_vad_status = findViewById(R.id.lb_vad_status);

        audioCapturer.setOnAudioCapturedListener(this);
    }

    public void onClick_record(View view) {
        if (sw_record.isChecked()){
            audioCapturer.startCapture();
        }else {
            audioCapturer.stopCapture();
        }
    }

    @Override
    public void onAudioCaptured(short[] audioData, int stamp) {
        final boolean vad_status = vad.process(16000,audioData);
        handler.post(new Runnable() {
            @Override
            public void run() {
                lb_vad_status.setText(vad_status ? "有声":"无声");
            }
        });
        pcmDataArr.add(audioData);
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
                short [] pcm_after = ns.process(pcm,pcm.length);
                return pcm_after;
            }
        });
    }

    public void onClick_agcPlay(View view) {
        playAudio(new IBerforePlayAudio() {
            @Override
            public short[] onBerforePlayAudio(short[] pcm) {
                WebRtcAgc.ResultOfProcess ret = agc.process(pcm,pcm.length,100,0);
                return ret.out;
            }
        });
    }

    private void playAudio(final IBerforePlayAudio cb){
        sw_record.setChecked(false);
        sw_record.setEnabled(false);
        audioCapturer.stopCapture();

        taskQuenu.async(new TaskQuenu.Task() {
            @Override
            public void run() throws TaskQuenu.ExitInterruptedException {
                audioPlayer.startPlayer();
                for (short[] pcm : pcmDataArr){
                    short[] pcm_after = cb.onBerforePlayAudio(pcm);
                    audioPlayer.play(pcm_after,0,pcm_after.length);
                }
                audioPlayer.stopPlayer();
            }
        });

    }

    private interface IBerforePlayAudio
    {
       short[] onBerforePlayAudio(short[] pcm);
    }
}
