package edu.skku.monet.NAFrequencyWarpper.Receiver;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import ca.uol.aig.fftpack.*;

/**
 * Created by owner on 2014-06-29.
 */
public class SCListener {
    public static final int frequency = 8000;
    public static final int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    public static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    public ComplexDoubleFFT transformer;
    public ComplexDoubleFFT cTransformer;
    int blockSize = 32768;

    public SCListener()
    {
        transformer = new ComplexDoubleFFT(blockSize/2);
        cTransformer = new ComplexDoubleFFT(blockSize/2);
    }


    public SCListener(int size)
    {
        transformer = new ComplexDoubleFFT(size/2);
        cTransformer = new ComplexDoubleFFT(size/2);
    }



}
