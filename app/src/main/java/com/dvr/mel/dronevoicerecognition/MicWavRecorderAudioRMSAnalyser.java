package com.dvr.mel.dronevoicerecognition;

/**************************************************************************************************
 *  MicWavRecorder in a nutshell:                                                                 *
 *      _ detect any Amplitude Spike on MicWavRecorder.streamBuffer using RMS method
 *        and change MicWavRecorder
 *
 *      *
 *************************************************************************************************/

public class MicWavRecorderAudioRMSAnalyser extends Thread
{

    private float SENSITIVITY; // in our usecase<=>4.F;
    // Used to detect volume spikes
    // trigger "spike detected" flag when the actual
    // buffer's average amplitude is [SENSIBILITY] times
    // higher than the last one
    // => Tweak it, increase it if the recording starts
    // "randomly", if its sensitivity is too high.

    private float bufferAvgAmp = 0; // buffer's average amplitude

    MicWavRecorderAudioRMSAnalyser()
    {


    }

    @Override
    public void run()
    {

    }

}
