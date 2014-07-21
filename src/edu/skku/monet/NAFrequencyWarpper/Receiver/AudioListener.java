package edu.skku.monet.NAFrequencyWarpper.Receiver;

import com.badlogic.gdx.audio.analysis.KissFFT;
import edu.skku.monet.NAFrequencyWarpper.MyActivity;
import edu.skku.monet.NAFrequencyWarpper.Receiver.*;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.badlogic.gdx.audio.analysis.FFT;
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
public class AudioListener extends AsyncTask<Void, Void, Void>{

    public static final int kFFTSIZE = 32768;
    public static final int kBUFFERSIZE = 32768;
    public static final int kSAMPLERATE = 44100;
    public static final int kHighSIZE = 225;

    Object[] d = new Object[2];

    private float[] freq_f;
    private double[] freq_db;
    private double[] freq_db_mix;
    private double[] freq_db_harmonic;
    float[] fft_cpx, tmpi, tmpr;
    double[] real = new double[kFFTSIZE / 2];
    double[] imag= new double[kFFTSIZE / 2];
    double[] cToTransform = new double[kFFTSIZE];

    FFT fft = new FFT(kFFTSIZE, kSAMPLERATE);

    @Override
    protected Void doInBackground(Void... params) {
        SCListener l = new SCListener();
        try
        {
            // AudioRecord를 설정하고 사용한다.
            freq_f = new float[kFFTSIZE];
            freq_db = new double[kFFTSIZE/2];
            freq_db_mix = new double[kFFTSIZE/2];
            freq_db_harmonic = new double[kFFTSIZE/2];
            int bufferSize = AudioRecord.getMinBufferSize(kSAMPLERATE, l.channelConfiguration, l.audioEncoding);

            AudioRecord audioRecord = new AudioRecord(
            MediaRecorder.AudioSource.MIC, kSAMPLERATE, l.channelConfiguration, l.audioEncoding, bufferSize);

            short[] buffer = new short[l.blockSize];
            double[] toTransform = new double[l.blockSize];

            audioRecord.startRecording();

            while(MyActivity.started){
                int bufferReadResult = audioRecord.read(buffer, 0, l.blockSize);

                for(int i = 0; i < l.blockSize && i < bufferReadResult; i++){
                    freq_f[i] = buffer[i];
                }

                fft.forward(freq_f);
                fft_cpx = fft.getSpectrum();
                tmpi = fft.getImaginaryPart();
                tmpr = fft.getRealPart();
                publishProgress();
            }

            audioRecord.stop();
        }catch(Throwable t){
            Log.e("AudioRecord", "Recording Failed");
        }

        return null;
    }


    @Override
    protected void onProgressUpdate(Void... params) {
        for(int i = 0; i < kFFTSIZE/2; i++)
        {
            real[i] = (double)tmpr[i];
            imag[i] = (double)tmpi[i];
            freq_db[i] = Math.sqrt((real[i]*real[i]) + (imag[i]*imag[i]));
        }
        addHarmonics();

        int max = frequencyIndex();
        boolean checka = checkFreq(25, max);
        boolean checkb = checkFreq(30, max);
        Log.e("[checkA]", "RESULT :: " + checka);
        Log.e("[checkB]", "RESULT :: " + checkb);
        Log.e("[aListener]", "MAX:: " + frequencyIndex() + "Hz");
    }

    protected void addHarmonics() {
        int max_harmonics = 3;
        int fftrange = kFFTSIZE/2;
        freq_db_harmonic = freq_db.clone();
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
