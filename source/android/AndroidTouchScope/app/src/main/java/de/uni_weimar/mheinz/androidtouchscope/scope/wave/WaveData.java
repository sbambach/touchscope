package de.uni_weimar.mheinz.androidtouchscope.scope.wave;

public class WaveData
{
    public int[] data;
    public double voltageScale;
    public double voltageOffset;

    public double timeScale;
    public double timeOffset;

    public WaveData()
    {
        data = null;
        voltageScale = 0.0;
        voltageOffset = 0.0;
        timeScale = 0.0;
        timeOffset = 0.0;
    }

    public WaveData(int[] data,
                    double voltageScale, double voltageOffset,
                    double timeScale,    double timeOffset)
    {
        this.data = data;
        this.voltageScale = voltageScale;
        this.voltageOffset = voltageOffset;
        this.timeScale = timeScale;
        this.timeOffset = timeOffset;
    }
}
