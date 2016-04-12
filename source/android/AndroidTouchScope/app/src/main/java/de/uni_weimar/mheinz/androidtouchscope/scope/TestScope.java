package de.uni_weimar.mheinz.androidtouchscope.scope;

import android.os.Handler;

import de.uni_weimar.mheinz.androidtouchscope.scope.wave.*;

public class TestScope implements BaseScope
{
    private static final int READ_RATE = 100;

   // private LimitedByteDeque mSampleList1 = new LimitedByteDeque(POOL_SIZE);
    private WaveRequestPool mWaves1 = new WaveRequestPool(POOL_SIZE);
    private WaveRequestPool mWaves2 = new WaveRequestPool(POOL_SIZE);
    private WaveRequestPool mWaves3 = new WaveRequestPool(POOL_SIZE);
    private TimeData mTimeData = new TimeData();

    private FakeWaveData mFakeWave1;
    private FakeWaveData mFakeWave2;
    private FakeWaveData mFakeWave3;

    private Handler mReadHandler = new Handler();
    private OnReceivedName mOnReceivedName;

    public TestScope()
    {
        mFakeWave1 = new FakeWaveData(59909.986179362626);
        mFakeWave2 = new FakeWaveData(36135.1315588236);
        mFakeWave3 = new FakeWaveData(48039.920311455244);
        mFakeWave1.isOn = true;
    }

    public void open(OnReceivedName onReceivedName)
    {
        mOnReceivedName = onReceivedName;
        doCommand(Command.GET_NAME, 0, false);
    }

    public void close()
    {
        stop();
    }

    public void start()
    {
        stop();
        mReadHandler.postDelayed(mReadRunnable, 0);
    }

    public void stop()
    {
        mReadHandler.removeCallbacks(mReadRunnable);
    }

    public boolean isConnected()
    {
        return true;
    }

    private String getName()
    {
        return "Test Scope";
    }

    public int doCommand(Command command, int channel, boolean force)
    {
        int val = 0;
        switch (command)
        {
            case IS_CHANNEL_ON:
                val = isChannelOn(channel) ? 1 : 0;
                break;
            case GET_NAME:
                String name = getName();
                if(mOnReceivedName != null)
                    mOnReceivedName.returnName(name);
                break;
        }
        return val;
    }

    public WaveData getWave(int chan)
    {
        WaveData waveData = null;
        switch(chan)
        {
            case 1:
                waveData = mWaves1.peek();
                break;
            case 2:
                waveData = mWaves2.peek();
                break;
            case 3:
                waveData = mWaves3.peek();
                break;
        }
        return waveData;
    }

    public TimeData getTimeData()
    {
        return mTimeData;
    }

    private boolean isChannelOn(int channel)
    {
        boolean isOn = false;
        switch (channel)
        {
            case 1:
                isOn = mFakeWave1.isOn;
                break;
            case 2:
                isOn = mFakeWave2.isOn;
                break;
            case 3:
                isOn = mFakeWave3.isOn;
                break;
        }

        return isOn;
    }

    private void generateTone(int channel)
    {
        WaveData waveData;
        FakeWaveData fakeWaveData;
        switch(channel)
        {
            case 1:
                waveData = mWaves1.requestWaveData();
                fakeWaveData = mFakeWave1;
                break;
            case 2:
                waveData = mWaves2.requestWaveData();
                fakeWaveData = mFakeWave2;
                break;
            case 3:
            default:
                waveData = mWaves3.requestWaveData();
                fakeWaveData = mFakeWave3;
                break;
        }

        int[] buffer = null;
        if(isChannelOn(channel))
        {
            buffer = new int[(int)(SAMPLE_LENGTH * mTimeData.timeScale)];
            double sampleRate = 10.0;
            double freq = fakeWaveData.freq;
            // double freq = Math.random() * 80000 + 10000;
            for (int cnt = (int)mTimeData.timeOffset, i = 0; i < buffer.length; cnt++, i++)
            {
                double time = cnt / sampleRate;
                double sinValue =
                        (Math.sin(2 * Math.PI * freq * time) +
                                Math.sin(2 * Math.PI * (freq / 1.8) * time) +
                                Math.sin(2 * Math.PI * (freq / 1.5) * time)) / 3.0;
                int byteValue = (byte) (125 * sinValue + 125);
                byteValue = (byteValue & 0xFF);
                byteValue = (int)(byteValue * fakeWaveData.scale + fakeWaveData.offset);
                if(byteValue > 255)
                    byteValue = 255;
                else if(byteValue < 0)
                    byteValue = 0;
                buffer[i] = byteValue;
            }
        }

        waveData.data = buffer;
        waveData.voltageScale = 1.0/fakeWaveData.scale;
        waveData.voltageOffset = fakeWaveData.offset;

        switch (channel)
        {
            case 1:
                mWaves1.add(waveData);
                break;
            case 2:
                mWaves2.add(waveData);
                break;
            case 3:
                mWaves3.add(waveData);
                break;
        }
    }

    private Runnable mReadRunnable = new Runnable()
    {
        @Override
        public void run()
        {
         //   if(mIsChan1On)
                generateTone(1);
        //    if(mIsChan2On)
                generateTone(2);
        //    if(mIsChan3On)
                generateTone(3);
            mReadHandler.postDelayed(this, READ_RATE);
        }
    };

    private class FakeWaveData
    {
        final double freq;
        double scale;
        double offset;
        boolean isOn;

        public FakeWaveData(double freq)
        {
            this.freq = freq;
            scale = 1;
            offset = 0;
            isOn = false;
        }
    }
}
