package com.dvr.mel.dronevoicerecognition;

// AudioRecord imports
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
// StreamBuffer Queue imports
import java.util.LinkedList;
import java.util.Queue;

/**************************************************************************************************
 *  MicWavRecorderHandler in a nutshell:                                                          *
 *      _ Initialize a "Microphone Input Stream" using AudioRecord                                *
 *      _ fill a Queue of streamBuffer containing Audio stream                                    *
 *      _ notify WavStreamHandler's thread every time a buffer is filled and queued,              *
 *        based on "Producer/Consumer" Algorithm                                                  *
 *      _                                                                                         *
 *                                                                                                *
 * Limitations: _ don't try to use multiple MicWavRecorders at the same time... Just don't, ok... *
 *                this class is implementing singleton design pattern anyway, so go crazy...      *
 *                That wouldn't make sense anyway to grab (and modify) mic input buffer           *
 *                from multiple MicWavRecorder threads anyways,                                   *
 *                and concurrent mic input accesses is also prohibited by Android anyways, so ... *
 *              _ Supporting only 16 bits Encoding format at the moment                           *
 *                GetMinBufferSize doesn't work with 8 bits, and 32 bits would require switch     *
 *                rewriting the streamBuffer and all subsequent code,                             *
 *                mainly adapting it to float support ... Not gonna happens ... No thanks         *
 *                When google fix their sh*** and enable 8bits supports,                          *
 *                then I'll come back for 32 bits. Your move creep                                *
 *************************************************************************************************/

/*****************************************
 * TODO List, what to tackle first:
 *          _ Make this class a singleton
 *          _ makes more "safe" thread closing using InteruptEvent
 */

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


    /**** Singleton and lock insurance ****/
    //MicWavRecorderHandler singletonInstance; //TODO when got time
    final Object lock = new Object(); // shared lock with WavStreamHandler for "producer/consumer" problem resolution

    /**** AudioRecord's settings (AUDIO FORMAT SETTINGS) ****/
    int SAMPLE_RATE; // in our usecase<=>16000, 16KHz
    int CHANNEL_MODE; // in our usecase<=>AudioFormat.CHANNEL_IN_MONO<=>mono signal
    int ENCODING_FORMAT; // in our usecase<=>AudioFormat.ENCODING_PCM_16BIT<=>16 bits
    private static int BUFFER_SIZE_MULTIPLICATOR = 10; // Used to define the Audio input's buffer size
                                                       // May need some empirical tweaking if for instance
                                                       // the recording trigger itself over a really short but loud Audio burst
    /**** Associated threads ****/
    MicActivity uiActivity; // Activity "linked to"/"which started" this MicWavRecorder //TODO maybe switch to private afterwards and static access to variables
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
                                                  // TODO : this is really basic thread management, to replace if enough time



    /***************************************************
     *                                                 *
     *           CONSTRUCTOR & "DESTRUCTOR"            *
     *                                                 *
     ***************************************************/



    MicWavRecorderHandler( int SAMPLE_RATE_, int CHANNEL_MODE_, int ENCODING_FORMAT_,
                    MicActivity uiActivity_) throws MicWavRecorderHandlerException
    {
//        if (singletonInstance == null)
//            singletonInstance = this; // TODO later and better

        // Initializing "USER DETERMINED VARIABLES"
        SAMPLE_RATE = SAMPLE_RATE_;
        CHANNEL_MODE = CHANNEL_MODE_;
        ENCODING_FORMAT = ENCODING_FORMAT_;

        //Microphone Initialization
        bufferSizeByte = BUFFER_SIZE_MULTIPLICATOR*AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MODE, ENCODING_FORMAT);
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
        if ( mic.getState() != AudioRecord.STATE_INITIALIZED ) throw new MicWavRecorderHandlerException("Couldn't instantiate AudioRecord properly");

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
    {
        // closing microphone
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
    {

        // Basic Producer(MicWavRecorderHandler) and Consumer(WavStreamHandler) problem

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
