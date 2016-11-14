package com.dvr.mel.dronevoicerecognition;

import android.media.AudioFormat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**************************************************************************************************
 *  MicWavRecorder in a nutshell:                                                                 *
 *      _ handle mic and IO stream
 *      _ determine if relevant or not
 *      _ detect any Amplitude Spike on MicWavRecorder.streamBuffer using RMS method
 *        and change MicWavRecorder
 *       _ Encapsulate mic's PCM audio input in a WAV file (dynamic header creation)               *

 *      *
 *************************************************************************************************/

public class WavStreamHandler extends Thread
{

    private float SENSITIVITY; // in our usecase<=>4.F;
    // Used to detect volume spikes
    // trigger "spike detected" flag when the actual
    // buffer's average amplitude is [SENSIBILITY] times
    // higher than the last one
    // => Tweak it, increase it if the recording starts
    // "randomly", if its sensitivity is too high.

    private MicWavRecorderHandler micHandler;

    private float bufferAvgAmp = 0; // buffer's average amplitude
    static private byte[] byteStreamBuffer; // streamBuffer converted into a byte buffer
    // doing this because outputStream can only work with byte[]
    static private short[] silenceBuffer; // "silence measurement" buffer, used to clear recordings
    // TODO : add clearAudio() function in the future to clear the signal by substracting silence to it
    private boolean alive = true;

    private long audioLength=0;
    // Output file and stream variable
    private File outputFile = null; // outputFile (should be something like
    // "/DATA/APP/com.dvr.mel.dronevoicerecognition/corpus/[UserName]/[orderName].wav"
    private FileOutputStream outputStream ; // stream used to fill the outputFile

    int bufferSize = micHandler.bufferSize; // TODO yes it's useless for the code
                                            // but it maintains what remains of my mental sanity


    WavStreamHandler(MicWavRecorderHandler micHandler_)
    {
        micHandler = micHandler_;

        silenceBuffer = new short[bufferSize];
        byteStreamBuffer = new byte[bufferSize*2];
        //TODO check state of mic, no need to start checking if buffer is useful before mic is even turned on

    }

    @Override
    public void run()
    {
        // Not much to do
        // This thread is called periodically by its static method
        // but other than that it has nothing to work on its own
        // TODO : There has to be a better design out there ..... big F to java for """"securing"""" their Thread
    }


    private void setUserSpeakingValue()
    {

        // if (getRMSValue() OP SENSITIVITY)
        //    toggle MicWavRecorder.userSpeaking
    }



    private double getRMSValue()
    {
        // return RMS value of streamBuffer
        double rmsVal=0.F;

        for( short s : micHandler.streamBuffer )
        {
            rmsVal+=s*s;
        }

        return Math.sqrt(rmsVal/bufferSize);
    }

    public void close()
    {
        //TODO
    }




//    void setOutput(String fileAddress)
//    {
//        // Set the outputFile of the mic stream
//        try
//        {
//            outputFile = new File(fileAddress);
//            outputStream = new FileOutputStream(outputFile, false); //overWrite the file if it exists
//
//            if (!outputFile.exists())
//                outputFile.createNewFile();
//
//            // write DUMMY Wav header, will be completed after the recording cause we need to know
//            // PCM Audio's Length before writing it
//            byte[] header = new byte[44];
//            outputStream.write(header, 0, 44);
//        }
//        catch (IOException e)
//        {
//            e.printStackTrace();
//        }
//    }



//    private void writeWavHeader()
//    {   // Write WAV header into outputStream according to "USER DETERMINED VARIABLES"
//        // refers to : http://soundfile.sapp.org/doc/WaveFormat/ for more information on WAV header
//
//        // calculating variables needed to complete headers
//        byte bitsPerSample;
//        switch (ENCODING_FORMAT)
//        {
//            case AudioFormat.ENCODING_PCM_8BIT : { bitsPerSample = 8; break;}
//            case AudioFormat.ENCODING_PCM_16BIT : { bitsPerSample = 16; break;}
//            case AudioFormat.ENCODING_PCM_FLOAT : { bitsPerSample = 32; break;}
//            default : { bitsPerSample = 0; }
//        }
//        long bytePerBlock = (CHANNEL_MODE == AudioFormat.CHANNEL_IN_STEREO) // determine the blockByteRate, how many bytes per block
//                ? (bitsPerSample * SAMPLE_RATE * 2 / 8) // stereo signal
//                : (bitsPerSample * SAMPLE_RATE / 8); // mono signal
//        long dataAndSubHeaderSize = audioLength+36;
//        int nbrOfChannel = (CHANNEL_MODE == AudioFormat.CHANNEL_IN_STEREO) ? '2' : '1';
//
//        // Completing header (little-indian
//        byte[] header = new byte[44];
//
//        // RIFF chunk descriptor
//        header[0]='R'; header[1] = 'I'; header[2]='F'; header[3]='F'; // RIFF (start of "RIFF" chunk descriptor)
//        // RIFF => use little-endian notation
//        header[4]=(byte) (dataAndSubHeaderSize & 0xff); header[5]=(byte) ((dataAndSubHeaderSize >> 8) & 0xff);
//        header[6]=(byte) ((dataAndSubHeaderSize >> 16) & 0xff); header[7]=(byte) ((dataAndSubHeaderSize >> 24) & 0xff);
//        // (file Size-8)
//        // <=> (AudioLength+36)
//        // <=> (AudioLength+RIFF chunk + "fmt" sub-chunk + ("data" subchunk-audioData) )
//        header[8] ='W'; header[9]='A'; header[10]='V'; header[11]='E'; // WAVE
//        // "fmt" sub-chunk
//        header[12]='f'; header[13]='m'; header[14]='t'; header[15]=' '; // fmt (start of "fmt" sub-chunk)
//        header[16]=16; header[17]='0'; header[18]='0'; header[19]='0'; // size of the fmt sub-chunk (minus the "fmt" start block 12->15) // 16 because it's PCM
//        header[20]='1'; header[21]='0'; // compression setting, 1<=> no compression
//        header[22]=(byte) nbrOfChannel; header[23]= 0;// number of channel
//        header[24]=(byte) (SAMPLE_RATE & 0xff); header[25]=(byte) ((SAMPLE_RATE >> 8) & 0xff);
//        header[26]=(byte) ((SAMPLE_RATE >> 16) & 0xff); header[27]=(byte) ((SAMPLE_RATE >> 24) & 0xff); // sample rate (KHz)
//        header[28]=(byte) (bytePerBlock & 0xff); header[29]=(byte) ((bytePerBlock >> 8) & 0xff);
//        header[30]=(byte) ((bytePerBlock >> 16) & 0xff); header[31]=(byte) ((bytePerBlock >> 24) & 0xff); // bytePerBlock
//        header[32]=(byte) (nbrOfChannel*bitsPerSample/8); header[33]='0'; // block alignment / number of bytes for one sample
//        header[34]=bitsPerSample; header[35]='0';// bitsPerSample
//        // "data" sub-chunk
//        header[36]='d'; header[37]='a'; header[38]='t'; header[39]='a'; // data (start of "data" sub-chunk)
//        header[40]=(byte) (audioLength & 0xff); header[41]=(byte) ((audioLength >> 8) & 0xff);
//        header[42]=(byte) ((audioLength >> 16) & 0xff); header[43]=(byte) ((audioLength >> 24) & 0xff); // Actual Audio Data (PCM) length
//
//        // Write completed header
//        try { outputStream.write(header, 0, 44); }
//        catch (IOException e) { e.printStackTrace(); }
//    }



}
