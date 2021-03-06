package com.dvr.mel.dronevoicerecognition;

// AudioRecord imports
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
// StreamBuffer Queue imports
import java.util.LinkedList;
import java.util.Queue;

/**************************************************************************************************
 *  MicWavRecorderHandler in a nutshell:                                                          *
 *      _ Initialize a "Microphone Input Stream" using AudioRecord                                *                                *
 *      _ Handle creation/destruction of a WavStreamHandler Thread to compute streamBuffers       *
 *      _ fill a Queue of streamBuffer containing Audio stream                                    *
 *      _ notify WavStreamHandler's thread every time a buffer is filled and queued,              *
 *        based on "Producer/Consumer" Algorithm                                                  *
 *                                                                                                *
 * Limitations: _ don't try to use multiple MicWavRecorders at the same time... Just don't, ok... *
 *                That wouldn't make sense anyway to grab (and modify) mic input buffer           *
 *                from multiple MicWavRecorder threads (or something else) anyways,               *
 *                and concurrent mic input accesses is also prohibited by Android anyways, so ... *
 *                I should probably use some "Initialization-on-demand holder" pattern            *
 *                combined with a Initializer function to emulate a Constructor with parameters   *
 *                .... But it just feels really really dirty.                                     *
 *                So for now I'll just assume devs using those classes are clever enough to read  *
 *                text box called "Limitations"                                                   *
 *              _ Supporting only 16 bits Encoding format at the moment                           *
 *                GetMinBufferSize doesn't work with 8 bits, and 32 bits would require switch     *
 *                rewriting the streamBuffer and all subsequent code,                             *
 *                mainly adapting it to float support ... Not gonna happens ... No thanks         *
 *                When google fix their sh*** and enable 8bits supports,                          *
 *                then I'll come back for 32 bits. Your move creep                                *
 *                                                                                                *
 *   Author : https://github.com/Aryetis                                                          *
 *************************************************************************************************/




// custom Exception
class MicWavRecorderHandlerException extends Exception
{
    MicWavRecorderHandlerException(String message)
    {
        super(message);
    }
}




class MicWavRecorderHandler extends Thread
{
    /***************************************************
     *                                                 *
     *                INTERN VARIABLES                 *
     *                                                 *
     ***************************************************/


    /**** Lock Object for Critical Section ****/
    static final Object lock = new Object(); // shared lock with WavStreamHandler for "producer/consumer" problem resolution

    /**** AudioRecord's settings (AUDIO FORMAT SETTINGS) ****/
    int SAMPLE_RATE; // in our usecase<=>16000, 16KHz
    int CHANNEL_MODE; // in our usecase<=>AudioFormat.CHANNEL_IN_MONO<=>mono signal
    int ENCODING_FORMAT; // in our usecase<=>AudioFormat.ENCODING_PCM_16BIT<=>16 bits

    /**** Associated threads ****/
    MicActivity uiActivity; // Activity "linked to"/"which started" this MicWavRecorder
    private WavStreamHandler audioAnalyser;
                                  // used to analyse mic's input buffer without blocking
                                  // this thread from filling it. ("Producer, Consumer" problem)
                                  // all Files IO and Audio Analysing are delegated over there

    /**** Audio associated variables ****/
    private AudioRecord mic; // "Mic Audio Input" Object
    private short[] streamBuffer; // buffer used to constantly listen to the mic
    int bufferSizeByte; // size of following buffers IN BYTE
    int bufferSizeElmt; // number of Element per buffer
    Queue<short[]> streamBufferQueue; // streamBuffer filled are pushed onto this Queue, waiting for their treatment

    /**** MicWavRecorder's lifespan variable ****/
    private volatile boolean runningState = true; // describe MicWavRecorder's lifespan
                                                  // by stopping its run() loop



    /***************************************************
     *                                                 *
     *           CONSTRUCTOR & "DESTRUCTOR"            *
     *                                                 *
     ***************************************************/



    MicWavRecorderHandler( int SAMPLE_RATE_, int CHANNEL_MODE_, int ENCODING_FORMAT_,
                    MicActivity uiActivity_) throws MicWavRecorderHandlerException
    {   // Initializing "USER DETERMINED VARIABLES"
        SAMPLE_RATE = SAMPLE_RATE_;
        CHANNEL_MODE = CHANNEL_MODE_;
        ENCODING_FORMAT = ENCODING_FORMAT_;

        //Microphone Initialization
        bufferSizeByte = AppInfo.BUFFER_SIZE_MULTIPLICATOR*AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MODE, ENCODING_FORMAT);
                    // value expressed in bytes
                    // using 10 times the getMinBufferSize to avoid IO operations and reduce a bad "producer / consumer" case's probabilities
        switch (ENCODING_FORMAT)
        {
            case AudioFormat.ENCODING_PCM_8BIT : { bufferSizeElmt = bufferSizeByte; break; }
            case AudioFormat.ENCODING_PCM_16BIT : { bufferSizeElmt = bufferSizeByte / 2; break; }
            default : { throw new MicWavRecorderHandlerException("Unsupported ENCODING_FORMAT"); }
        }
        mic = new AudioRecord( MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, CHANNEL_MODE,
                ENCODING_FORMAT, bufferSizeByte );
                // mic always on, completing a non-circular buffer
                // use audioAnalyser (WavStreamHandler) to detect if buffer is relevant or not
                //     <=> if phone is recording silence or not.
        if ( mic.getState() != AudioRecord.STATE_INITIALIZED )
            throw new MicWavRecorderHandlerException("Couldn't instantiate AudioRecord properly");

        // Initializing streamBufferQueue
        streamBufferQueue = new LinkedList<>();

        // Initializing buffers
        streamBuffer = new short[bufferSizeElmt];

        // Link current MivWavRecorder's thread to its MicActivity's thread
        uiActivity = uiActivity_;

        // Initialize and start the WavStreamHandler's thread that will detect audio's spikes
        audioAnalyser = new WavStreamHandler(this);
        audioAnalyser.start();

        // Start recording with the mic
        mic.startRecording();
    }



    void close()
    {   // closing microphone
        mic.stop();
        mic.release();

        //closing AudioAnalyser
        audioAnalyser.close();

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
    {   // Basic Producer(MicWavRecorderHandler) and Consumer(WavStreamHandler) problem
        while(runningState)
        {
            // update streamBuffer / produce a streamBuffer
            mic.read(streamBuffer, 0, bufferSizeElmt);// read() IS A BLOCKING METHOD !!!
                                                      // it will wait for the buffer to be filled before returning it

            synchronized(lock) // CRITICAL SECTION : synchronize on the same lock with Consumer
            {
                streamBufferQueue.add(streamBuffer); // add buffer to the Queue shared with Consumer
                lock.notify(); // notify consumer that a streamBuffer is ready to be consumed
            }
        }
    }


}
