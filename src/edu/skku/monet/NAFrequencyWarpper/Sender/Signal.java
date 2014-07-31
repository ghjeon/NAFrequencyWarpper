package edu.skku.monet.NAFrequencyWarpper.Sender;

/**
 * Created by owner on 2014-06-29.
 */
public class Signal {
    public double interval = 0.8;
    public int duration = 3;
    public int samplingRate = 44100;
    public int numofSamples = 4096;
    public int frequency = FreqencyConstatns.SendAB[0];
    public double sample[] = new double[numofSamples];


    public byte generatedSnd[] = new byte[2 * numofSamples];

    public Signal(int frequency)
    {
        this.frequency = frequency;
        double amplitude = 0.25;
        double theta = 0;
        double theta_increment = 2.0 * Math.PI * frequency / samplingRate;

        for(int i = 0; i < numofSamples; i++)
        {
            sample[i] = Math.sin(theta) * amplitude;
            theta += theta_increment;

            if(theta > 2.0 * Math.PI)
                theta -= 2.0 * Math.PI;
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
    }
}
