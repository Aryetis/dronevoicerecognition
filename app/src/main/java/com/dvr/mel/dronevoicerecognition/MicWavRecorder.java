package com.dvr.mel.dronevoicerecognition;

import android.media.AudioFormat;
import android.media.AudioRecord; // must use AudioRecord and not MediaRecorder
                                  // because MediaRecorder only allow us to work on (compressed)
                                  // file (not stream) once the recording is over
import android.media.MediaRecorder; // only used to aceed int MIC
import android.util.Log; // TODO : remove at the end of the dev

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Aryetis on 11/11/16.
 * TODO : _ create mic inputStream
 *        _ autodetect when user talks
 *        _ get the input stream of the user talking
 *        _ clean it using "silence sample"
 *        _ throw some nice equalizer in there
 * IDEA : _ find a way to calibrate the mic => register silence and substract it to recordings
 */

/*
 * Limitations : Can only write stereo or mono wav, no 5.0, 5.1 or other weird input
 *               SAMPLE_RATE must be storable on 4 bytes ... should not be a problem except if you record at some bat shit crazy sample rate
 */

class MicWavRecorder extends Thread
{
    /**** USER DETERMINED VARIABLES ***/
    // Sensibility settings
    private float DURATION_WINDOW; // in our usecase<=>1.F for 1 second
                                   // duration of the sample used to detect spike / detect
                                   // when the user speaks
                                   // empirical determined valuee
                                   // => Tweak it, increase if the sentence are cut
                                   // in multiple words
    private float SENSITIVITY; // in our usecase<=>4.F;
                               // Used to detect volume spikes
                               // trigger "spike detected" flag when the actual
                               // buffer's average amplitude is [SENSIBILITY] times
                               // higher than the last one
                               // => Tweak it, increase it if the recording starts
                               // "randomly", if its sensitivity is too high.

    // AudioRecord's settings (file format settings)
    private long SAMPLE_RATE; // in our usecase<=>16000, 16KHz // stored in a long cause it's stored as such in a wav header
    private int CHANNEL_MODE; // in our usecase<=>AudioFormat.CHANNEL_IN_MONO<=>mono signal
    private int ENCODING_FORMAT; // in our usecase<=>AudioFormat.ENCODING_PCM_16BIT<=>16 bits

    /**** INTERN CLASS VARIABLES ***/
    // AudioRecorders and buffers
    private AudioRecord mic;
    private int bufferLength; // length of streamBuffer and silenceBuffer
    private short[] streamBuffer; // buffer used to constantly listen to the mic
    private byte[] byteStreamBuffer; // streamBuffer converted into a byte buffer
                                     // doing this because outputStream can only work with byte[]
    private short[] silenceBuffer; // "silence measurement" buffer, used to clear recordings
                                   // TODO : add clearAudio() function in the future to clear the signal by substracting silence to it
    private long audioLength=0;
    // Output file and stream variable
    private File outputFile; // outputFile (should be something like
                     // "/DATA/APP/com.dvr.mel.dronevoicerecognition/corpus/[UserName]/[orderName].wav"
    private FileOutputStream outputStream ; // stream used to fill the outputFile
    // Running state machine's variable
    private volatile boolean runningState = true; // bool used to stop the thread
    private boolean userSpeaking = false; // bool used to determine whether the user is speaking or not
    // Mess
    private float bufferAvgAmp = 0; // buffer's average amplitude



    MicWavRecorder(float DURATION_WINDOW_, float SENSITIVITY_, long SAMPLE_RATE_,
                          int CHANNEL_MODE_, int ENCODING_FORMAT_
                  )
    {
        // set Thread priority to high to avoid it being canceled by Android because shenanigans
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        // setting USER DETERMINED VARIABLES
        DURATION_WINDOW = DURATION_WINDOW_;
        SENSITIVITY = SENSITIVITY_;
        SAMPLE_RATE = SAMPLE_RATE_;
        CHANNEL_MODE = CHANNEL_MODE_;
        ENCODING_FORMAT = ENCODING_FORMAT_;

        // setting INTERN CLASS VARIABLES
        mic = new AudioRecord( MediaRecorder.AudioSource.MIC,
                                (int)SAMPLE_RATE,
                                CHANNEL_MODE,
                                ENCODING_FORMAT,
                                (int)(SAMPLE_RATE*DURATION_WINDOW)); // mic always on
                                                                     // firing burst of small buffers
                                                                     // using detectAudioSpike to detect if buffer is relevant or not
        bufferLength = (int)(SAMPLE_RATE*DURATION_WINDOW); // setting the length of our buffers
        streamBuffer = new short[bufferLength]; // buffer used to constantly listen to the mic
                                                // in our usecase<=>1 second long
        byteStreamBuffer = new byte[bufferLength*2]; // need twice as many element cause we converting 2 bytes into 2*1 byte
        silenceBuffer = new short[bufferLength]; // "silence measurement" buffer, used to clear recordings

        // starting mic
        mic.startRecording();
    }



    public void run()
    { // called by start() due to Java's Thread System

        /***********************************************
         *                                              *
         *      main method, here goes the Magiku!      *
         *                                              *
         ***********************************************/

        while(runningState) // TODO modify so we only loop when buffer is full
        {
            detectAudioSpike(); // detect audio Spike <=> update userSpeaking value

            if(userSpeaking)
            {   // user is speaking
                writeStreamBufferToOutputStream(); // write streamBuffer into outputStream
            }
            else
            {   // user stopped talking
                // => write audiofile && update its header
                //    && get new filepath to write into or END_SIGNAL //TODO END_SIGNAL
                writeStreamBufferToOutputStream(); // write streamBuffer into outputStream
            }
        }
    }



    void close()
    {
        // stopping the Thread loop
        runningState = false;
        // closing microphone
        mic.stop();
        // close FileOutputStream
        try
        {
            outputStream.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        // stop the thread ... Yes that's how you do it in Java nowadays with a volatile variable
        // ... It's fugly
        runningState = false;
    }



    /***********************************************
     *                                              *
     *      Microphone / Audio related methods      *
     *                                              *
     ***********************************************/




    private boolean detectAudioSpike()
    {
        // Detect an amplitude spike in the Mic input stream (sudden silence, sudden noise)
        // And write accordingly in the outputStream
        // Also update the userSpeaking value for the run loop

        float newBufferAvgAmp=0.F;

        // Get current Mic Audio's buffer
        try
        {
            if ( mic.read(streamBuffer, 0, (int)(SAMPLE_RATE*DURATION_WINDOW)) < 0)
            // fill the streamBuffer with the current audio sample
            // return negative value if something went wrong
                throw new IOException();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        // Calculate buffer's Average Amplitude
        for ( short s : streamBuffer )
        {
            newBufferAvgAmp+=s;
        }
        newBufferAvgAmp /= bufferLength;

        // Determine whether there is a spike or not and act accordingly
        if ( newBufferAvgAmp > bufferAvgAmp*SENSITIVITY )
        {   // increasing spike detected <=> user started talking

Log.i("MicWavRecorder", "User started talking");
            // flushing streamBuffer into the outputStream
            writeStreamBufferToOutputStream(); // write streamBuffer into outputStream
            // updating userSpeaking's value for the run loop
            userSpeaking = true;
            // updating bufferAvgAmp
            bufferAvgAmp = newBufferAvgAmp;
            return true;
        }
        if ( newBufferAvgAmp < bufferAvgAmp/SENSITIVITY )
        {   // decreasing spike detected <=> user stopped talking

Log.i("MicWavRecorder", "User stoped talking");
            // flushing streamBuffer into the outputStream
            writeStreamBufferToOutputStream(); // write streamBuffer into outputStream
            try
            {
                // actualizing audioLength for the wav header construction
                audioLength = outputStream.getChannel().size();
                outputStream.flush(); // flush outputStream into the file
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            // updating the header
            writeWavHeader();
            // updating userSpeaking's value for the run loop
            userSpeaking = false;
            // updating bufferAvgAmp
            bufferAvgAmp = newBufferAvgAmp;
            return true;
        }
        // no spike detected
        // => update silence buffer && update bufferAvgAmp
        silenceBuffer = streamBuffer;
        bufferAvgAmp = newBufferAvgAmp;
        return false;
    }



    void record(String fileAdress)
    {
        // Set the outputFile of the mic stream
        try
        {
            outputFile = new File(fileAdress);
            outputStream = new FileOutputStream(outputFile, false); //overWrite the file if it exists

            if (!outputFile.exists())
                outputFile.createNewFile();

            // write DUMMY Wav header, will be completed after the recording cause we need to know
            // PCM Audio's Length before writing it
            byte[] header = new byte[44];
            outputStream.write(header, 0, 44);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void writeWavHeader()
    {
        // calculating variables needed to complete headers
        byte bitsPerSample;
        switch (ENCODING_FORMAT)
        {
            case AudioFormat.ENCODING_PCM_8BIT : { bitsPerSample = 8; break;}
            case AudioFormat.ENCODING_PCM_16BIT : { bitsPerSample = 16; break;}
            case AudioFormat.ENCODING_PCM_FLOAT : { bitsPerSample = 32; break;}
            default : { bitsPerSample = 0; }
        }
        long bytePerBlock = (CHANNEL_MODE == AudioFormat.CHANNEL_IN_STEREO) // determine the blockByteRate, how many bytes per block
                ? (bitsPerSample * SAMPLE_RATE * 2 / 8) // stereo signal
                : (bitsPerSample * SAMPLE_RATE / 8); // mono signal
        long dataAndSubHeaderSize = audioLength+36;
        int nbrOfChannel = (CHANNEL_MODE == AudioFormat.CHANNEL_IN_STEREO) ? '2' : '1';

        // Completing header (little-indian
        byte[] header = new byte[44];

        // RIFF chunk descriptor
        header[0]='R'; header[1] = 'I'; header[2]='F'; header[3]='F'; // RIFF (start of "RIFF" chunk descriptor)
                                                                        // RIFF => use little-endian notation
        header[4]=(byte) (dataAndSubHeaderSize & 0xff); header[5]=(byte) ((dataAndSubHeaderSize >> 8) & 0xff);
        header[6]=(byte) ((dataAndSubHeaderSize >> 16) & 0xff); header[7]=(byte) ((dataAndSubHeaderSize >> 24) & 0xff);
                    // (file Size-8)
                    // <=> (AudioLength+36)
                    // <=> (AudioLength+RIFF chunk + "fmt" sub-chunk + ("data" subchunk-audioData) )
        header[8] ='W'; header[9]='A'; header[10]='V'; header[11]='E'; // WAVE
        // "fmt" sub-chunk
        header[12]='f'; header[13]='m'; header[14]='t'; header[15]=' '; // fmt (start of "fmt" sub-chunk)
        header[16]=16; header[17]='0'; header[18]='0'; header[19]='0'; // size of the fmt sub-chunk (minus the "fmt" start block 12->15) // 16 because it's PCM
        header[20]='1'; header[21]='0'; // compression setting, 1<=> no compression
        header[22]=(byte) nbrOfChannel; header[23]= 0;// number of channel
        header[24]=(byte) (SAMPLE_RATE & 0xff); header[25]=(byte) ((SAMPLE_RATE >> 8) & 0xff);
        header[26]=(byte) ((SAMPLE_RATE >> 16) & 0xff); header[27]=(byte) ((SAMPLE_RATE >> 24) & 0xff); // sample rate (KHz)
        header[28]=(byte) (bytePerBlock & 0xff); header[29]=(byte) ((bytePerBlock >> 8) & 0xff);
        header[30]=(byte) ((bytePerBlock >> 16) & 0xff); header[31]=(byte) ((bytePerBlock >> 24) & 0xff); // bytePerBlock
        header[32]=(byte) (nbrOfChannel*bitsPerSample/8); header[33]='0'; // block alignement / number of bytes for one sample
        header[34]=bitsPerSample; header[35]='0';// bitsPerSample
        // "data" sub-chunk
        header[36]='d'; header[37]='a'; header[38]='t'; header[39]='a'; // data (start of "data" sub-chunk)
        header[40]=(byte) (audioLength & 0xff); header[41]=(byte) ((audioLength >> 8) & 0xff);
        header[42]=(byte) ((audioLength >> 16) & 0xff); header[43]=(byte) ((audioLength >> 24) & 0xff); // Actual Audio Data (PCM) length


        // Write completed header
        try
        {
            outputStream.write(header, 0, 44);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    private void writeStreamBufferToOutputStream()
    {  // convert the streamBuffer short array into a byte array
       // and write it into outputStream
        int curShortIndex = 0, curByteIndex = 0;
        int iterations = bufferLength;

        // iterate over the short Array, for each iteration create and insert two bytes
        for(; curShortIndex != iterations ;)
        {
            byteStreamBuffer[curByteIndex] = (byte) (streamBuffer[curShortIndex] & 0x00ff);
            byteStreamBuffer[curByteIndex+1] = (byte) ((streamBuffer[curShortIndex] & 0x00ff) >> 8);

            ++curShortIndex; curByteIndex += 2;
        }

        // Write byteStreamBuffer ( should be identical to streamBuffer ) into outputStream
        try
        {
            outputStream.write(byteStreamBuffer);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }


}
