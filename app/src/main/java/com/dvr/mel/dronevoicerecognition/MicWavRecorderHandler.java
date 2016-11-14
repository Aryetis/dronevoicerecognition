package com.dvr.mel.dronevoicerecognition;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

/**************************************************************************************************
 *  MicWavRecorderHandler in a nutshell:                                                          *
 *      _ Initialize a "Microphone Input Stream" using AudioRecord                                *
 *      _ smart detect when the user is talking using RMS to detect audio Amplitude spikes        *
 *          (using WavStreamHandler) this is done in another thread                 *
 *          because calculating average amplitude is obviously gonna takes times                  *
 *          doing the job in another thread we will not loose AudioStream informations for        *
 *          the time it takes to do the job and will resolve a                                    *
 *          "Consumer/Producer" problem when filling the buffer                                   *
 *      _ Handle creation and destruction of output Files                                         *
 *      _ Notify MicTestActivity's linked Activity when a recording is over                       *
 *      // TODO actualize this, kinda based on MVC architecture
 *                                                                                                *
 *                                                                                                *
 * Limitations : don't try to use multiple MicWavRecorders at the same time... Just don't, ok ... *
 *               that wouldn't make sense anyway to grab (and modify) mic input buffer            *
 *               from multiple MicWavRecorder threads anyways,                                    *
 *               and concurrent mic input access is also prohibited                               *
 *************************************************************************************************/

/*****************************************
 * TODO List, what to tackle first:
 *          _ detectAudioSpike simili function
 *          _ creation and suppression of file
 *          _ recording of audio stream in those file
 *          _ kill WavStreamHandler thread in close()
 *          _ chill out
 */

// custom Exception
class MicWavRecorderHandlerException extends Exception
{
    public MicWavRecorderHandlerException(String message)
    {
        super(message);
    }
}




public class MicWavRecorderHandler extends Thread
{
    /***************************************************
     *                                                 *
     *                INNER VARIABLES                  *
     *                                                 *
     ***************************************************/

    /**** AudioRecord's settings (AUDIO FORMAT SETTINGS) ***/
    private long SAMPLE_RATE; // in our usecase<=>16000, 16KHz // stored in a long cause it's stored as such in a wav header
    private int CHANNEL_MODE; // in our usecase<=>AudioFormat.CHANNEL_IN_MONO<=>mono signal
    private int ENCODING_FORMAT; // in our usecase<=>AudioFormat.ENCODING_PCM_16BIT<=>16 bits

    /**** intern routines's variables ***/
    // AudioRecorder and buffers
    private AudioRecord mic;
    public int bufferSize; // size of following buffers
    public static short[] streamBuffer; // buffer used to constantly listen to the mic

    // Running state machine's variable
    // Associated threads
    private MicTestActivity activity; // Activity "linked to"/"which started" this MicWavRecorder
    private WavStreamHandler audioAnalyser = new WavStreamHandler(this);
                                  // used to analyse mic's input buffer without blocking
                                  // this thread from filling it. ("Producer, Consumer" problem)
    // MicWavRecorder's state variables
    private volatile boolean runningState = true; // describe MicWavRecorder's lifespan
                                                  // by stopping its run() loop
                                                  // TODO : this is really basic thread management, to replace if enough time
    private static boolean userSpeaking = false; // boolean describing if user is currently speaking or not (using audioAnalyser)
    private static boolean finishedRecordingCurrentCommand = false; // TODO


    /***************************************************
     *                                                 *
     *           CONSTRUCTOR & "DESTRUCTOR"            *
     *                                                 *
     ***************************************************/


    MicWavRecorderHandler( long SAMPLE_RATE_, int CHANNEL_MODE_, int ENCODING_FORMAT_,
                    MicTestActivity activity_) throws MicWavRecorderHandlerException
    {
        // Initializing "USER DETERMINED VARIABLES"
        SAMPLE_RATE = SAMPLE_RATE_;
        CHANNEL_MODE = CHANNEL_MODE_;
        ENCODING_FORMAT = ENCODING_FORMAT_;

        //setting "INTERN CLASS VARIABLES"
        //Microphone Initialization
        bufferSize = 10*AudioRecord.getMinBufferSize((int)SAMPLE_RATE, CHANNEL_MODE, ENCODING_FORMAT);
                    // value expressed in bytes
                    // using 10 times the getMinBufferSize to avoid IO operations and reduce a bad "producer / consumer" case's probabilities
        mic = new AudioRecord( MediaRecorder.AudioSource.MIC,
                (int)SAMPLE_RATE, CHANNEL_MODE,
                ENCODING_FORMAT, bufferSize );
                // mic always on, completing a non-circular buffer
                // use audioAnalyser (WavStreamHandler) to detect if buffer is relevant or not
                //     <=> if phone is recording silence or not.
        Log.i("MicWavRecorder", "State"+mic.getState()); // check that AudioRecord has been correctly instantiated
        if ( mic.getState() != AudioRecord.STATE_INITIALIZED ) throw new MicWavRecorderHandlerException("Couldn't instantiate AudioRecord properly");

        // Initializing buffers
        streamBuffer = new short[bufferSize];

        // Link current MivWavRecorder's thread to its MicTestActivity's thread
        activity = activity_;

        // Set output file and stream
        // setOutput(fileName); // TODO : YES !!! SET FIRST fileOutput in onCreate
                                // and set the next one at the END OF RECORDING,
                                // don't waste time and resources waiting for the second recording to start

        // Start the WavStreamHandler's thread that will detect audio's spikes
        audioAnalyser.start();

        // Start recording with the mic
        mic.startRecording();
    }



    public void close()
    {
        // closing microphone
        mic.stop();
        mic.release();

        //closing AudioAnalyser
        audioAnalyser.close();

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
        while(runningState)
        {
            // update streamBuffer
            mic.read(streamBuffer, 0, bufferSize); // read() IS A BLOCKING METHOD !!!
                                                   // it will wait for the buffer to be filled before returning it

            // Analyse streamBuffer (on another thread to not loose informations during its treatment)
            // TODO FIRST !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

            // Eventually request for next command to evaluate (or be killed by MicTestActivity)
            // TODO move this part to the WavStreamHandler thread to loose ZERO time between each streamBuffer update
            // TODO SECOND !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            if ( finishedRecordingCurrentCommand )
            {   //request next Command .... must go throught runOnUiThread because
                // even with activity being MicTestActivity (aka the UI)
                // android traces back the call from a "non UI" context and refuses to access Views
                activity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run() {
                        activity.nextCommand();
                    }
                });
                finishedRecordingCurrentCommand = false;
            }
//DEBUG
// update UI
//activity.runOnUiThread(new Runnable() {
//@Override
//public void run() {
//    activity.nextCommand();
//}
//});
        }
    }



    /***************************************************
     *                                                 *
     *              METHODS DECLARATION                *
     *                                                 *
     ***************************************************/




}
