package com.dvr.mel.dronevoicerecognition;

import android.content.Context;
import android.content.ContextWrapper;
import android.media.AudioFormat;
// Stream specific imports
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

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
 *          _ fix corrupted audio files
 *
 *
 *          _ Make this class a singleton
 *          _ makes more "safe" thread closing using InteruptEvent
 *          _ Add forced pause(500ms) in between words to recalibrate silence ?
 */



class WavStreamHandler extends Thread
{
    /***************************************************
     *                                                 *
     *                INTERN VARIABLES                 *
     *                                                 *
     ***************************************************/

    /**** Singleton and lock insurance ****/
    //MicWavRecorderHandler singletonInstance; //TODO when got time

    /**** Associated thread ****/
    private MicWavRecorderHandler micHandler;

    /**** Audio associated variables ****/
    float SENSITIVITY = 5.F;  // (Empirical value) Used to detect when User start/stop talking
                                      // the switch triggers when
                                      // ( "currentBuffer's RMS" > "previousBuffer's RMS" * SENSITIVITY )
                                      // RMS : Average RMS Amplitude value
                                      // => Tweak it if the recording starts "randomly" or user needs to yell at the mic
                                      // TODO : allow user to modify this value in some "OptionActivity"
    private double silenceAvgRMSAmp = 0; // silence's average amplitude
    private int bufferSizeByte; // bufferSizeByte = micHandler.bufferSizeByte; // size of following buffers IN BYTE
    private int bufferSizeElmt; // bufferSizeElmt = micHandler.bufferSizeElmt; // number of Element per buffer
    static private short[] streamBuffer; // copy of a queued streamBuffer, because we don't want to
                                         // hold the producer's thread in hostage during our computing process
                                         // basic "Producer/Consumer" protocol stuff
    private long audioLength; // Total length in bytes of the currently recorded PCM Audio's stream

    /**** State machine states variables ****/
    private boolean userSpeaking = false; // boolean describing if user is currently speaking or not (using audioAnalyser)

    /**** File Output and File stream variables ****/
    private String commandName; // text of the current command being recorded (eg: "Avance", "Recule", etc)
    private File corpusDir; // corpus's specific directory ( should be something like [corpusGlobalDir]/corpusName/ )
    private File commandFile; // outputFile's path ( should be something like [corpusDir]/[orderName].wav" )
    private FileOutputStream fos ; // stream used to fill the outputFile
    private DataOutputStream dos; // middle-man stream between a short[] and a File
                                  // because there is no direct way to write a short[] to a File .... Thanks Java

    /**** WavStreamHandler's lifespan variable ****/
    private volatile boolean runningState = true;

    /**** UI Critical Section Routine ****/
    private final Runnable nextCommandRoutine = new Runnable()
    {
        @Override
        public void run()
        {
            micHandler.uiActivity.nextCommand();
            synchronized (this) { this.notify(); }
        }
    };




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
        bufferSizeByte = micHandler.bufferSizeByte;
        bufferSizeElmt = micHandler.bufferSizeElmt;
        streamBuffer = new short[bufferSizeElmt];

// TODO DEBUG \/ to be removed and use Global Variables after merging
// get Application's Context
ContextWrapper cw = new ContextWrapper(this.micHandler.uiActivity.getApplicationContext());
// get Application's data subfolder directory
File baseDir = cw.getDir("data", Context.MODE_PRIVATE);
// create Global Corpus subdirectory
File corpusGlobalDir = new File(baseDir, "Corpus");
if ( !corpusGlobalDir.exists())
   corpusGlobalDir.mkdir();


        // Set output file and stream
        // create specific corpus's subdirectory
        corpusDir = new File(corpusGlobalDir, MicActivity.corpusName);
        if ( !corpusDir.exists())
            try
            {
                if ( ! corpusDir.mkdir() )
                    throw new IOException("Couldn't create the following directory : "+corpusDir);
            }
            catch ( IOException ie ) { ie.printStackTrace(); }
        // Update file's output
        commandName = micHandler.uiActivity.getCurrentCommandName();
        setOutput( commandName+".wav" );
    }



    void close()
    {
        // close FileOutputStream and DataOutputStream
        try { dos.close(); fos.close();}
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
                // dequeuing streamBuffer from the Queue, immediately copy it in order to not hold back the "producer"
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
        /**** First silence calibration ****/
        if ( silenceAvgRMSAmp == 0 )
        {   // if silenceAvgRMSAmp has'nt been initialized
            // just calibrate the silence value
            silenceAvgRMSAmp = getRMSValue();
            return;
        }

        // Acquiring current Buffer RMS Average Amplitude
        double newBufferAvgRMSAmp = getRMSValue();

        /**** Detect if ( "User starts talking" ) ****/
        if ( newBufferAvgRMSAmp >= silenceAvgRMSAmp*SENSITIVITY && !userSpeaking )
        {
            // Switch userSpeaking's state flag
            userSpeaking = true;

            // Update UI (toggle progress bar circle thingy)
            toggleUIRecordingStateValue();

            // Start recording
            writeStreamBuffer();

            return;
        }

        /**** Detect if ( "User stops talking" ) ****/
        if ( newBufferAvgRMSAmp < silenceAvgRMSAmp*SENSITIVITY && userSpeaking )
        {
            // Switch userSpeaking's state flag
            userSpeaking = false;

            // Update UI (only toggle progress bar circle thingy)
            toggleUIRecordingStateValue();

            // Finish current recording, flush and close outputStream
            try
            {
                writeStreamBuffer();
                writeWavHeader(); // Complete file's Wav header
                fos.close();
            }
            catch (IOException ie)
            { ie.printStackTrace(); }

            // Update the current Command to the next one (and update UI accordingly)
            // modifying curCommandIndex, used right after that to get new commandName,
            //    => Thus synchronized section
            // AND modifying UI element from non-UI context
            //    => Thus runOnUIThread subroutine
            // Praise the all mighty """"Java security"""" at its finest ...
            synchronized ( nextCommandRoutine )
            {
                micHandler.uiActivity.runOnUiThread( nextCommandRoutine) ;
                try { nextCommandRoutine.wait(); } catch (Exception e) { e.printStackTrace();}
            }

            // Set next file Output
            commandName = micHandler.uiActivity.getCurrentCommandName(); // get new Command name

            if ( commandName != null ) // getCurrentCommandName() returns null if going OOB / reaching the end of the List
                // if Activity successfully switched to the next Command to record in the list
                // aka we still have new Files to record => set next outputFile
                setOutput( commandName+".wav" );

            // update silenceAvgRMSAmp
            silenceAvgRMSAmp = newBufferAvgRMSAmp;
            return;
        }

        /**** Detect if ( "User is still talking ") ****/
        if ( userSpeaking )
        {
            // Continue recording
            writeStreamBuffer();

            return;
        }

        /**** Detect if ( "User is STILL NOT talking ") ****/
        if ( !userSpeaking )  // go home Intelij you're drunk ... this variable is NOT always true
        {
            // update bufferAvgRMSAmp
            silenceAvgRMSAmp = newBufferAvgRMSAmp;
        }
    }



    private double getRMSValue()
    {
        // return RMS value of streamBuffer
        double rmsVal=0.F;

        for( short s : streamBuffer )
            rmsVal+=s*s;

        return Math.sqrt(rmsVal/(bufferSizeElmt));
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



    /***************************************************
     *                                                 *
     *             OUTPUT FILES ROUTINES               *
     *                                                 *
     ***************************************************/



    private boolean setOutput(String outputFileName)
    {   // Set the output file of the Audio stream
        // Note : ".wav" extension should be added at the call of the method
        boolean returnValue=true;

        try
        {
            // create command's File
            commandFile = new File(corpusDir, outputFileName);
            if ( !commandFile.exists() )
                returnValue = commandFile.createNewFile();

            // create FileOutputStream
            fos = new FileOutputStream(commandFile, false); //overWrite the file if it exists
            // create DataOutputStream
            dos = new DataOutputStream(fos);

            // write DUMMY Wav header, will be completed after the recording cause we need to know
            // PCM Audio's Length before writing it
            byte[] header = new byte[44];
            fos.write(header, 0, 44);
        }
        catch (IOException e)
        { e.printStackTrace(); }

        return returnValue;
    }



    private void writeStreamBuffer()
   {   // Write the current short[] buffer into the file going through a DataOutputStream
        // thus there is no need to convert those short into bytes manually

       // update audioLength
       audioLength += bufferSizeByte; // audioLength <=> side in byte of actual PCM audio data ;
                                      // bufferSizeByte <=> size in byte of streamBuffer / NOT number of element in streamBuffer
       try
       {
            for (short s : streamBuffer)
                   dos.writeShort( Short.reverseBytes(s) ); // "data" has to been written in little-indian fasion
            dos.flush(); // flush current buffer into fos=>file
       } catch (IOException ie) { ie.printStackTrace(); }
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
        int nbrOfChannel = (micHandler.CHANNEL_MODE == AudioFormat.CHANNEL_IN_STEREO) ? 2 : 1;

        // Completing header (little-indian
        byte[] header = new byte[44];

        // RIFF chunk descriptor
        header[0]='R'; header[1] = 'I'; header[2]='F'; header[3]='F'; // RIFF (start of "RIFF" chunk descriptor)
        // RIFF => use little-endian notation for non ascii stuff
        header[4]=(byte) (dataAndSubHeaderSize & 0xff); header[5]=(byte) ((dataAndSubHeaderSize >> 8) & 0xff);
        header[6]=(byte) ((dataAndSubHeaderSize >> 16) & 0xff); header[7]=(byte) ((dataAndSubHeaderSize >> 24) & 0xff);
        // (file Size-8)
        // <=> (AudioLength+36)
        // <=> (AudioLength+RIFF chunk + "fmt" sub-chunk + ("data" subchunk-audioData) )
        header[8] ='W'; header[9]='A'; header[10]='V'; header[11]='E'; // WAVE
        // "fmt" sub-chunk
        header[12]='f'; header[13]='m'; header[14]='t'; header[15]=' '; // fmt (start of "fmt" sub-chunk)
        header[16]=16; header[17]=0; header[18]=0; header[19]=0; // size of the fmt sub-chunk (minus the "fmt" start block 12->15) // 16 because it's PCM
        header[20]=1; header[21]=0; // compression setting, 1<=> no compression
        header[22]=(byte) nbrOfChannel; header[23]= 0;// number of channel
        header[24]=(byte) (micHandler.SAMPLE_RATE & 0xff); header[25]=(byte) ((micHandler.SAMPLE_RATE >> 8) & 0xff);
        header[26]=(byte) ((micHandler.SAMPLE_RATE >> 16) & 0xff); header[27]=(byte) ((micHandler.SAMPLE_RATE >> 24) & 0xff); // sample rate (KHz)
        header[28]=(byte) (bytePerBlock & 0xff); header[29]=(byte) ((bytePerBlock >> 8) & 0xff);
        header[30]=(byte) ((bytePerBlock >> 16) & 0xff); header[31]=(byte) ((bytePerBlock >> 24) & 0xff); // bytePerBlock
        header[32]=(byte) (nbrOfChannel*bitsPerSample/8); header[33]=0; // block alignment / number of bytes for one sample
        header[34]=bitsPerSample; header[35]=0;// bitsPerSample
        // "data" sub-chunk
        header[36]='d'; header[37]='a'; header[38]='t'; header[39]='a'; // data (start of "data" sub-chunk)
        header[40]=(byte) (audioLength & 0xff); header[41]=(byte) ((audioLength >> 8) & 0xff);
        header[42]=(byte) ((audioLength >> 16) & 0xff); header[43]=(byte) ((audioLength >> 24) & 0xff); // Actual Audio Data (PCM) length

        // Write completed header using randomAccess
        try
        {
            RandomAccessFile rafOut = new RandomAccessFile(commandFile.getAbsolutePath(), "rw");
            rafOut.seek(0);
            rafOut.write(header);
        }
        catch ( Exception e ) { e.printStackTrace(); }

        // Reset file specific variables
        audioLength = 0;
    }



}
