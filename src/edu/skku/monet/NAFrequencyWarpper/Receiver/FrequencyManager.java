package edu.skku.monet.NAFrequencyWarpper.Receiver;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;

/**
 * Project IntelliJ IDEA
 * Module edu.skku.monet.NAFrequencyWarpper.Receiver
 * User: Gyuhyeon
 * Date: 2014. 7. 31.
 * Time: 오전 12:48
 */
public class FrequencyManager {

    private static int MAX_COL = 31;

    private int col = MAX_COL;

    private int index = 0;


    private boolean[] frequencyStatus;

    Timer jobScheduler = new Timer();


    public FrequencyManager()
    {
        frequencyStatus = new boolean[MAX_COL];
        jobScheduler.scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run()
            {
                clear();
            }
        }, 0, 5000);

    }

    public FrequencyManager(int row, int col)
    {
        this.col = col;
        frequencyStatus = new boolean[col];
    }

    private void clear()
    {
        frequencyStatus = new boolean[col];
    }

    public void updateFrequency(int frequencyIndex)
    {
        frequencyStatus[frequencyIndex] = true;
    }

    public boolean validFrequency(int freqA, int freqB)
    {
        if(frequencyStatus[freqA] && frequencyStatus[freqB])
            return true;
        else
            return false;
    }

}
