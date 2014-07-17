package edu.skku.monet.NAFrequencyWarpper.Receiver;

import edu.skku.monet.NAFrequencyWarpper.MyActivity;
import edu.skku.monet.NAFrequencyWarpper.Receiver.*;
import android.os.*;
import android.media.*;
import android.util.Log;


/**
 * Project IntelliJ IDEA
 * Module edu.skku.monet.NAFrequencyWarpper.Receiver
 * User: Gyuhyeon
 * Date: 2014. 7. 17.
 * Time: 오후 11:49
 */
public class AudioListener extends AsyncTask<Void, double[], Void>{

    public static final int kFFTSIZE = 32768;
    public static final int kBUFFERSIZE = 32768;
    public static final int kSAMPLERATE = 44100;
    public static final int kHighSIZE = 225;

    Object[] d = new Object[2];

    private double[] freq_db;
    private double[] freq_db_mix;
    private double[] freq_db_harmonic;
    double[] cToTransform = new double[kFFTSIZE];

    @Override
    protected Void doInBackground(Void... params) {
        SCListener l = new SCListener();
        try
        {
            // AudioRecord를 설정하고 사용한다.
            freq_db = new double[kFFTSIZE/2];
            freq_db_mix = new double[kFFTSIZE/2];
            freq_db_harmonic = new double[kFFTSIZE/2];
            int bufferSize = AudioRecord.getMinBufferSize(l.frequency, l.channelConfiguration, l.audioEncoding);

            AudioRecord audioRecord = new AudioRecord(
            MediaRecorder.AudioSource.MIC, l.frequency, l.channelConfiguration, l.audioEncoding, bufferSize);


                // short로 이뤄진 배열인 buffer는 원시 PCM 샘플을 AudioRecord 객체에서 받는다.
                // double로 이뤄진 배열인 toTransform은 같은 데이터를 담지만 double 타입인데, FFT 클래스에서는 double타입이 필요해서이다.
            short[] buffer = new short[l.blockSize];
            double[] toTransform = new double[l.blockSize];

            audioRecord.startRecording();

            while(MyActivity.started){
                int bufferReadResult = audioRecord.read(buffer, 0, l.blockSize);

                    // AudioRecord 객체에서 데이터를 읽은 다음에는 short 타입의 변수들을 double 타입으로 바꾸는 루프를 처리한다.
                    // 직접 타입 변환(casting)으로 이 작업을 처리할 수 없다. 값들이 전체 범위가 아니라 -1.0에서 1.0 사이라서 그렇다
                    // short를 32,768.0(Short.MAX_VALUE) 으로 나누면 double로 타입이 바뀌는데, 이 값이 short의 최대값이기 때문이다.
                for(int i = 0; i < l.blockSize && i < bufferReadResult; i++){
                    toTransform[i] = (double)buffer[i] / Short.MAX_VALUE; // 부호 있는 16비트
                }
                cToTransform = toTransform;
                    // 이제 double값들의 배열을 FFT 객체로 넘겨준다. FFT 객체는 이 배열을 재사용하여 출력 값을 담는다. 포함된 데이터는 시간 도메인이 아니라
                    // 주파수 도메인에 존재한다. 이 말은 배열의 첫 번째 요소가 시간상으로 첫 번째 샘플이 아니라는 얘기다. 배열의 첫 번째 요소는 첫 번째 주파수 집합의 레벨을 나타낸다.

                    // 256가지 값(범위)을 사용하고 있고 샘플 비율이 8,000 이므로 배열의 각 요소가 대략 15.625Hz를 담당하게 된다. 15.625라는 숫자는 샘플 비율을 반으로 나누고(캡쳐할 수 있는
                    // 최대 주파수는 샘플 비율의 반이다. <- 누가 그랬는데...), 다시 256으로 나누어 나온 것이다. 따라서 배열의 첫 번째 요소로 나타난 데이터는 영(0)과 15.625Hz 사이에
                    // 해당하는 오디오 레벨을 의미한다.
                l.transformer.ft(toTransform);

                    // publishProgress를 호출하면 onProgressUpdate가 호출된다.
                publishProgress(toTransform);
                try
                {
                    Thread.sleep(100);
                } catch (Exception e)
                {

                }
            }

            audioRecord.stop();
        }catch(Throwable t){
            Log.e("AudioRecord", "Recording Failed");
        }

        return null;
    }


    @Override
    protected void onProgressUpdate(double[]... toTransform) {
        for(int i = 0; i < kFFTSIZE/2; i++)
        {
            freq_db[i] = Math.sqrt((double)toTransform[0][i*2]*10000 * (double)toTransform[0][i*2]*10000 +
                    (double)toTransform[0][i*2+1]*10000 * (double)toTransform[0][i*2+1]*10000) / (kFFTSIZE/2);
        }
        addHarmonics();

        /*
        for(int i = 0; i < toTransform[0].length; i++){
            int x = i;
            int downy = (int) (100 - (toTransform[0][i] * 10));
            int upy = 100;
            int hz = (int)(toTransform[0][i]*100);
            if(hz > 1000)
                Log.e("[aListener]", hz + "Hz");
        }
        */
        Log.e("[aListener]", "MAX:: " + frequencyIndex() + "Hz");
    }

    protected double hammingWindow(int totalSamples, int input) {
        double a = 2.0 * 3.141592654 / ( totalSamples - 1 );
        double w;

        w = 0.54 - 0.46 * Math.cos( a * input );
        return w;
    }

    protected void performWindow(short[] buffer, int totalSamples) {
        for (int i = 0; i < totalSamples; i++){
            buffer[i] *= hammingWindow(totalSamples, i);
        }
    }

    protected void addHarmonics() {
        int max_harmonics = 3;
        int fftrange = kFFTSIZE/2;
        for(int j = 2; j <= max_harmonics; j++){
            int low_bin = 0;
            double new_value = 0;
            for(int i = 0; i < fftrange; i++){
                int next_bin = (int)Math.round((double)i / (double)j);

                if(next_bin > low_bin){
                    freq_db_harmonic[low_bin] += new_value;
                    low_bin = next_bin;
                    new_value = 0;
                }

                new_value = Math.max(new_value, freq_db[i]);
            }
        }
        int j = 13000;
        for (int i=0; i < kHighSIZE; i++) {
            freq_db_mix[i] = (freq_db_harmonic[j-14]+freq_db_harmonic[j-13]*2+freq_db_harmonic[j-12]*4+freq_db_harmonic[j-11]*8+freq_db_harmonic[j-10]*16+freq_db_harmonic[j-9]*32+freq_db_harmonic[j-8]*64+freq_db_harmonic[j-7]*128+freq_db_harmonic[j-6]*256+freq_db_harmonic[j-5]*512+freq_db_harmonic[j-4]*1024+freq_db_harmonic[j-3]*2048+freq_db_harmonic[j-2]*4096+freq_db_harmonic[j-1]*8192+freq_db_harmonic[j]*16384*2+freq_db_harmonic[j+1]*8192+freq_db_harmonic[j+2]*4096+freq_db_harmonic[j+3]*2048+freq_db_harmonic[j+4]*1024+freq_db_harmonic[j+5]*512+freq_db_harmonic[j+6]*256+freq_db_harmonic[j+7]*128+freq_db_harmonic[j+8]*64+freq_db_harmonic[j+9]*32+freq_db_harmonic[j+10]*16+freq_db_harmonic[j+11]*8+freq_db_harmonic[j+12]*4+freq_db_harmonic[j+13]*2+freq_db_harmonic[j+14])/100000;
            j = j+15;
            if (j >= ((kFFTSIZE/2) - 15)) {
                break;
            }
        }
    }

    protected int findTopSpike() {
        double max = 0;
        int max_index = 0;
        for(int i = 0; i < kHighSIZE; i++){
//		double db = freq_db_harmonic[i];
            double db = freq_db_mix[i];
            if(db > max){
                max = db;
                max_index = i;
            }
        }
        return max_index;
    }

    protected boolean findCheckFrequency(int _check) {
        double db = freq_db_mix[_check];
        if(db > 500)
            return true;
        else
            return false;
    }

    protected int frequencyIndex() {
        int freq_index = findTopSpike();

        // 인덱스를 더 잘 찾기 위해 앞에꺼와 뒤에꺼를 더해서

        if (freq_db_mix[freq_index] > 10) {
            return freq_index;
        }
        else {
            return 0;
        }
    }

    protected boolean checkFreq(int aFreq, int aMax) {
        if (freq_db_mix[aFreq-1] > 10 || freq_db_mix[aFreq] > 10 || freq_db_mix[aFreq+1] > 10) {
            if (aMax == aFreq-1 || aMax == aFreq || aMax == aFreq+1) {
                return true;
            }
            else {
                return false;
            }
        }
        else {
            return false;
        }
    }
}
