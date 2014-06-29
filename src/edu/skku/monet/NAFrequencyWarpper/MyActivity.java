package edu.skku.monet.NAFrequencyWarpper;

import android.app.Activity;
import android.widget.LinearLayout;
import android.view.ViewGroup;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.Context;
import android.media.*;
import android.os.Bundle;
import edu.skku.monet.NAFrequencyWarpper.Sender.*;

public class MyActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    private button serviceButton = null;

    class button extends Button {
        /* PlayButton의 기능 정의. Button 으로부터 상속받아 구현됨 */

        boolean mStartPlaying = true; // 현재 레코딩 상황을 보관함

        /* EventListener. Button Object에 대한 onClick Event, 즉 사용자가 버튼을 눌렀을 경우에 관한 Event를 통제하는 Listener. */
        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                sendSignal();
            }
        };

        public button(Context ctx) {  // Play Button Constructor
            super(ctx);
            setText("Start");
            setOnClickListener(clicker);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout ll = new LinearLayout(this); // 레이아웃 종류 선택
        serviceButton = new button(this);  // 레코드 버튼 객체 생성
        ll.addView(serviceButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0)); // 앞서 선안한 레이아웃에 버튼 추가함
        setContentView(ll); // 어플리케이션 화면에 레이아웃 출력



    }

    private void sendSignal()
    {
        Signal s;
        for(int len = 0; len < FreqencyConstatns.SendAB.length; len++) {
            s = new Signal(FreqencyConstatns.SendAB[len]);
            final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    s.samplingRate, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, s.generatedSnd.length,
                    AudioTrack.MODE_STATIC);
            audioTrack.write(s.generatedSnd, 0, s.generatedSnd.length);
            audioTrack.play();
            try {
                Thread.sleep(80);
            }catch (Exception e) {

            }
        }
    }
}
