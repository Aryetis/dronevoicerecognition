package com.dvr.mel.dronevoicerecognition;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/************************************************************************************************
 *  MicWavRecorder in a nutshell:                                                               *
 *      _ TODO WRITE SOMETHING CLEVER HERE                                                      *
 *                                                                                              *
 ************************************************************************************************/

/*****************************************
 * TODO List, what to tackle first:
 *          _ detectAudioSpike simili function
 *          _ creation and suppression of file
 *          _ recording of audio stream in those file
 *          _ chill out
 */

// custom Exception
class MicWaveRecorderException extends Exception
{
    public MicWaveRecorderException(String message)
    {
        super(message);
    }
}

public class MicWavRecorder extends Thread
{
    /***************************************************
     *                                                 *
     *                INNER VARIABLES                  *
     *                                                 *
     ***************************************************/

    /**** USER DETERMINED VARIABLES ***/
    // Sensibility settings (empiric values)
    // private float DURATION_WINDOW; // expressed in second
                                   // TODO currently based on getMinBufferSize, maybe need to tweak it, input it manually later on
                                   // in our usecase<=>1.F for 1 second
                                   // duration of the sample used to detect spike / detect
                                   // when the user speaks
                                   // empirical determined value
                                   // => Tweak it, increase if the sentence are cut
                                   // in multiple words
    private float SENSITIVITY; // in our usecase<=>4.F;
                               // Used to detect volume spikes
                               // trigger "spike detected" flag when the actual
                               // buffer's average amplitude is [SENSIBILITY] times
                               // higher than the last one
                               // => Tweak it, increase it if the recording starts
                               // "randomly", if its sensitivity is too high.

    // AudioRecord's settings (AUDIO FORMAT SETTINGS)
    private long SAMPLE_RATE; // in our usecase<=>16000, 16KHz // stored in a long cause it's stored as such in a wav header
    private int CHANNEL_MODE; // in our usecase<=>AudioFormat.CHANNEL_IN_MONO<=>mono signal
    private int ENCODING_FORMAT; // in our usecase<=>AudioFormat.ENCODING_PCM_16BIT<=>16 bits

    /**** INTERN CLASS VARIABLES ***/
    // AudioRecorder and buffers
    private AudioRecord mic;
    private int bufferSize; // expressed in second
    private int bufferLength; // length of streamBuffer and silenceBuffer
    private short[] streamBuffer; // buffer used to constantly listen to the mic
    private byte[] byteStreamBuffer; // streamBuffer converted into a byte buffer
    // doing this because outputStream can only work with byte[]
    private short[] silenceBuffer; // "silence measurement" buffer, used to clear recordings
    // TODO : add clearAudio() function in the future to clear the signal by substracting silence to it
    private long audioLength=0;
    // Output file and stream variable
    private File outputFile = null; // outputFile (should be something like
    // "/DATA/APP/com.dvr.mel.dronevoicerecognition/corpus/[UserName]/[orderName].wav"
    private FileOutputStream outputStream ; // stream used to fill the outputFile
    // Running state machine's variable
    private volatile boolean runningState = true; // bool used to stop the thread (must be volatile)
    private boolean userSpeaking = false; // bool used to determine whether the user is speaking or not
    // Mess
    private float bufferAvgAmp = 0; // buffer's average amplitude
    private MicTestActivity activity; // Activity "linked to"/"which started" this MicWavRecorder


    /***************************************************
     *                                                 *
     *           CONSTRUCTOR & "DESTRUCTOR"            *
     *                                                 *
     ***************************************************/


    MicWavRecorder(float DURATION_WINDOW_, float SENSITIVITY_, long SAMPLE_RATE_,
                   int CHANNEL_MODE_, int ENCODING_FORMAT_, MicTestActivity activity_) throws MicWaveRecorderException
    {
        // Initializing "USER DETERMINED VARIABLES"
        SENSITIVITY = SENSITIVITY_;
        SAMPLE_RATE = SAMPLE_RATE_;
        CHANNEL_MODE = CHANNEL_MODE_;
        ENCODING_FORMAT = ENCODING_FORMAT_;

        //setting "INTERN CLASS VARIABLES"
        //Microphone Initialization
        bufferSize = 2*AudioRecord.getMinBufferSize((int)SAMPLE_RATE, CHANNEL_MODE, ENCODING_FORMAT);
        mic = new AudioRecord( MediaRecorder.AudioSource.MIC,
                (int)SAMPLE_RATE,
                CHANNEL_MODE,
                ENCODING_FORMAT,
                bufferSize );
        //(int)(SAMPLE_RATE*DURATION_WINDOW));
                                // mic always on, completing a circular buffer
                                // use detectAudioSpike() to detect if buffer is relevant or not
        Log.i("MicWavRecorder", "State"+mic.getState()); // check that AudioRecord has been correctly instantiated
        if ( mic.getState() != AudioRecord.STATE_INITIALIZED ) throw new MicWaveRecorderException("Couldn't instantiate AudioRecord properly");

        // Initializing buffers
        silenceBuffer = new short[bufferSize];
        streamBuffer = new short[bufferSize];
        byteStreamBuffer = new byte[bufferSize*2];
// Legacy code (may become useful again if we need to tweak bufferSize / "record windows size")
// bufferLength = (int)(SAMPLE_RATE*DURATION_WINDOW); // setting the length of our buffers
// streamBuffer = new short[bufferLength]; // buffer used to constantly listen to the mic
// // in our usecase<=>1 second long
// byteStreamBuffer = new byte[bufferLength*2]; // need twice as many element cause we converting 2 bytes into 2*1 byte
// silenceBuffer = new short[bufferLength]; // "silence measurement" buffer, used to clear recordings

        // Link current MivWavRecorder to a MicTestActivity .... Kinda fugly but this workaround will do for now
        activity = activity_;

        // Set output file and stream
        // setOutput(fileName); // TODO emplacement à revoir, maybe should be fixed only when spikeDetected

        // Start recording with the mic
        mic.startRecording();

    }



    public void close()
    {
        // closing microphone
        mic.stop();
        mic.release();

        // close FileOutputStream
        //try { outputStream.close(); } //TODO reenable when File creation is handled correctly
        //catch (IOException e) { e.printStackTrace(); }

        // stop the run loop / thread
        runningState = false;
    }



    /***************************************************
     *                                                 *
     *                   RUN LOOP                      *
     *                                                 *
     ***************************************************/



    @Override
    public void run()
    {
        while(true)
        {
            activity.recordNextCommand();
        }
    }



    /***************************************************
     *                                                 *
     *              METHODS DECLARATION                *
     *                                                 *
     ***************************************************/



    private void writeWavHeader()
    {   // Write WAV header into outputStream according to "USER DETERMINED VARIABLES"
        // refers to : http://soundfile.sapp.org/doc/WaveFormat/ for more information on WAV header

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
        header[32]=(byte) (nbrOfChannel*bitsPerSample/8); header[33]='0'; // block alignment / number of bytes for one sample
        header[34]=bitsPerSample; header[35]='0';// bitsPerSample
        // "data" sub-chunk
        header[36]='d'; header[37]='a'; header[38]='t'; header[39]='a'; // data (start of "data" sub-chunk)
        header[40]=(byte) (audioLength & 0xff); header[41]=(byte) ((audioLength >> 8) & 0xff);
        header[42]=(byte) ((audioLength >> 16) & 0xff); header[43]=(byte) ((audioLength >> 24) & 0xff); // Actual Audio Data (PCM) length

        // Write completed header
        try { outputStream.write(header, 0, 44); }
        catch (IOException e) { e.printStackTrace(); }
    }
}
