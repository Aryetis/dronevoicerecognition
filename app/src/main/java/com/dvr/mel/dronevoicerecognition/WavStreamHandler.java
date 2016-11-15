package com.dvr.mel.dronevoicerecognition;

import android.media.AudioFormat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**************************************************************************************************
 *  MicWavRecorder in a nutshell:                                                                 *
 *      _ evaluate mic stream, determine if it is relevant or not (silence) using RMS method      *
 *      _ handle IO stream, create PCM RIFF Wav files (dynamic header creation)                   *
 *      _ keep track of a silenceBuffer (for future optimized clean up algorithm)                 *
 *      _ triggers UI update based on mic stream                                                  *
 *                                                                                                *
 *************************************************************************************************/

/*****************************************
 * TODO List, what to tackle first:
 *          _ Make this class a singleton
 *          _ check private / public variables
 *          _ makes more "safe" thread closing using InteruptEvent
 *          _ Add forced pause(500ms) in between words to recalibrate silence ?
 */



public class WavStreamHandler extends Thread
{
    /***************************************************
     *                                                 *
     *                INNER VARIABLES                  *
     *                                                 *
     ***************************************************/

    /**** Singleton and lock insurance ****/
    //MicWavRecorderHandler singletonInstance; //TODO when got time

    /**** Associated thread ****/
    private MicWavRecorderHandler micHandler;

    /**** Audio associated variables ****/
    private float SENSITIVITY = 2.F;  // (Empirical value) Used to detect when User start/stop talking
                                      // the switch triggers when
                                      // ( "currentBuffer's RMS" > "previousBuffer's RMS" * SENSITIVITY )
                                      // RMS : Average RMS Amplitude value
                                      // => Tweak it if the recording starts "randomly" or user needs to yell at the mic
                                      // TODO : allow user to modify it in some "OptionActivity"
    private double bufferAvgRMSAmp = 0; // buffer's average amplitude
    int bufferSize; // bufferSize = micHandler.bufferSize;  yes it's redundant but more clear that way
    static private short[] streamBuffer; // copy of a queued streamBuffer, because we don't want to
                                         // hold the producer's thread in hostage during our computing process
                                         // basic "Producer/Consumer" protocol stuff
    static private byte[] byteStreamBuffer; // streamBuffer converted into a byte buffer
                                            // doing this because outputStream can only work with byte[]
    static private short[] silenceBuffer; // "silence measurement" buffer, used to clear recordings
    // TODO : add clearAudio() function in the future to clear the signal by substracting silence average value, specter analysis
    private long audioLength=0; // Total length in bytes of the currently recorded PCM Audio's stream TODO: check that it's in bytes

    /**** State machine states variables ****/
    private boolean userSpeaking = false; // boolean describing if user is currently speaking or not (using audioAnalyser)
    private boolean finishedRecordingCurrentCommand = false;

    /**** File Output and File stream variables ****/
    private File outputFile; // outputFile (should be something like [AppFolder]/[UserName]/[orderName].wav" )
    private FileOutputStream outputStream ; // stream used to fill the outputFile

    /**** WavStreamHandler's lifespan variable ****/
    private volatile boolean runningState = true;



    /***************************************************
     *                                                 *
     *           CONSTRUCTOR & "DESTRUCTOR"            *
     *                                                 *
     ***************************************************/



    WavStreamHandler(MicWavRecorderHandler micHandler_)
    {
        // Note : AudioRecord mic's stream is assumed to be already correctly initialized and recording

        //        if (singletonInstance == null)
//            singletonInstance = this; // TODO later and better

        // Link WavStreamHandler's Thread with MicWavRecorderHandler's Thread
        micHandler = micHandler_;

        // Initializing intern variables
        bufferSize = micHandler.bufferSize;
        streamBuffer = new short[bufferSize];
        silenceBuffer = new short[bufferSize];
        byteStreamBuffer = new byte[bufferSize*2];


        // Set output file and stream
        setOutput(micHandler.uiActivity.appFolderName+micHandler.uiActivity.corpusName+"/"+
                micHandler.uiActivity.getCurrentCommandName()+".wav");
    }



    public void close()
    {
        // close FileOutputStream
        try { outputStream.close(); }
        catch (IOException e) { e.printStackTrace(); }

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

        while (runningState)
        {
            synchronized (micHandler.lock) // CRITICAL SECTION : synchronize on the same lock with Producer
            {
                while (micHandler.streamBufferQueue.peek() == null)
                {   // while streamBufferQueue is empty / nothing to consume =>  wait
                    try { micHandler.lock.wait(); } catch (InterruptedException ie) { ie.printStackTrace();}
                }
                // dequeuing streamBuffer from the Queue, immediatly copy it in order to not hold back the "producer"
                streamBuffer = micHandler.streamBufferQueue.remove();
            }

            // Consume the streamBuffer asynchronously
            computeStreamBuffer();
        }
    }


    /***************************************************
     *                                                 *
     *          AUDIO STREAM ANALYSIS ROUTINES         *
     *                                                 *
     ***************************************************/


    private void computeStreamBuffer()
    {
        // First silence calibration
        if ( bufferAvgRMSAmp == 0 )
        {   // if we analyse the first streamBuffer passed by the mic
            // just calibrate the silence value
            bufferAvgRMSAmp = getRMSValue();
            return;
        }

        // Acquiring current Buffer RMS Average Amplitude
        double newBufferAvgRMSAmp = getRMSValue();

        // Detect if ( "User starts talking" )
        if ( newBufferAvgRMSAmp > bufferAvgRMSAmp*SENSITIVITY )
        {
            // Switch userSpeaking's state flag
            userSpeaking = true;

            // Start recording
            writeStreamBufferToOutputStream();

            // update bufferAvgRMSAmp
            bufferAvgRMSAmp = newBufferAvgRMSAmp;
            return;
        }

        // Detect if ( "User stops talking" )
        if ( newBufferAvgRMSAmp < bufferAvgRMSAmp*SENSITIVITY )
        {
            // Switch userSpeaking's state flag
            userSpeaking = false;

            // Finish current recording, flush and close outputStream
            try
            {
                writeStreamBufferToOutputStream();
                outputStream.flush();
                writeWavHeader(); // Complete file's Wav header
                outputStream.close();
            }
            catch (IOException ie)
            { ie.printStackTrace(); }

            if ( micHandler.uiActivity.nextCommand() )
            { // if Activity successfully switched to the next Command to record in the list
              // aka we still have new Files to record

                // set next outputFile
                setOutput(micHandler.uiActivity.appFolderName+micHandler.uiActivity.corpusName+"/"+
                          micHandler.uiActivity.getCurrentCommandName()+".wav");

                // actualize UI
                micHandler.uiActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        micHandler.uiActivity.nextCommand();
                    }
                });
            }


            // update bufferAvgRMSAmp
            bufferAvgRMSAmp = newBufferAvgRMSAmp;
            return;
        }

        // Detect if ( "User is still talking ")
        if ( userSpeaking )
        {
            // Continue recording
            writeStreamBufferToOutputStream();

            // update bufferAvgRMSAmp
            bufferAvgRMSAmp = newBufferAvgRMSAmp;
            return;
        }

        // Detect if ( "User is still NOT talking ")
        if ( !userSpeaking )
        {
            // Update silenceBuffer
            silenceBuffer = streamBuffer;

            // update bufferAvgRMSAmp
            bufferAvgRMSAmp = newBufferAvgRMSAmp;
            return;
        }
    }



    private double getRMSValue()
    {
        // return RMS value of streamBuffer
        double rmsVal=0.F;

        for( short s : streamBuffer )
            rmsVal+=s*s;

        return Math.sqrt(rmsVal/bufferSize);
    }




    /***************************************************
     *                                                 *
     *            UI UPDATE CALLS ROUTINES             *
     *                                                 *
     ***************************************************/


    private void toggleUIRecordingStateValue()
    {
        micHandler.uiActivity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            { micHandler.uiActivity.toggleRecordingState() ; }
        });
    }

    private void goToNextCommand()
    {
        // update UI
        micHandler.uiActivity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            { micHandler.uiActivity.nextCommand(); }
        });
    }



    /***************************************************
     *                                                 *
     *             OUTPUT FILES ROUTINES               *
     *                                                 *
     ***************************************************/



    void setOutput(String fileAdress)
    {
        // Set the output file of the Audio stream
        try
        {
Log.i("WavStreamHandler", "Setting output file to : "+fileAdress );
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
        { e.printStackTrace(); }
    }



    private void writeStreamBufferToOutputStream()
    {    // convert the streamBuffer short array into a byte array
        // and write it into outputStream
        int curShortIndex = 0, curByteIndex = 0;
        int iterations = micHandler.bufferSize;

        // iterate over the short Array, for each iteration create and insert two bytes
        for(; curShortIndex != iterations ;)
        {
            byteStreamBuffer[curByteIndex] = (byte) (streamBuffer[curShortIndex] & 0x00ff);
            byteStreamBuffer[curByteIndex+1] = (byte) ((streamBuffer[curShortIndex] & 0x00ff) >> 8);

            ++curShortIndex; curByteIndex += 2;
        }

        // Write byteStreamBuffer ( should be identical to streamBuffer ) into outputStream
        try
        { outputStream.write(byteStreamBuffer); }
        catch (IOException e)
        { e.printStackTrace(); }
    }



    private void writeWavHeader()
    {   // Write WAV header into outputStream according to "USER DETERMINED VARIABLES"
        // refers to : http://soundfile.sapp.org/doc/WaveFormat/ for more information on WAV header

        // calculating variables needed to complete headers
        byte bitsPerSample;
        switch (micHandler.ENCODING_FORMAT)
        {
            case AudioFormat.ENCODING_PCM_8BIT : { bitsPerSample = 8; break;}
            case AudioFormat.ENCODING_PCM_16BIT : { bitsPerSample = 16; break;}
            case AudioFormat.ENCODING_PCM_FLOAT : { bitsPerSample = 32; break;}
            default : { bitsPerSample = 0; }
        }
        long bytePerBlock = (micHandler.CHANNEL_MODE == AudioFormat.CHANNEL_IN_STEREO) // determine the blockByteRate, how many bytes per block
                ? (bitsPerSample * micHandler.SAMPLE_RATE * 2 / 8) // stereo signal
                : (bitsPerSample * micHandler.SAMPLE_RATE / 8); // mono signal
        long dataAndSubHeaderSize = audioLength+36;
        int nbrOfChannel = (micHandler.CHANNEL_MODE == AudioFormat.CHANNEL_IN_STEREO) ? '2' : '1';

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
        header[24]=(byte) (micHandler.SAMPLE_RATE & 0xff); header[25]=(byte) ((micHandler.SAMPLE_RATE >> 8) & 0xff);
        header[26]=(byte) ((micHandler.SAMPLE_RATE >> 16) & 0xff); header[27]=(byte) ((micHandler.SAMPLE_RATE >> 24) & 0xff); // sample rate (KHz)
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
