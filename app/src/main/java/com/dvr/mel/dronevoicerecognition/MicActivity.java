package com.dvr.mel.dronevoicerecognition;

// UI imports
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.media.AudioFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
// Corpus management imports
import java.io.File;
import java.util.ArrayList;
import java.util.List;



/**************************************************************************************************
 *  MicActivity in a nutshell:                                                                *
 *      _ get corpusName and corpusList from Parent Activity's Intent                             *
 *      _ pass corpusName to child Activity's Intent                                              *
 *      _ Use MicWavRecorderHandler (& WavStreamHandler) to record                                *
 *        a list of Wav audio files according to corpusList                                       *
 *                                                                                                *
 *  Sidenotes :                                                                                   *
 *  _ If it helps, see Mic related Activities as MVC designed activities,                         *
 *    with MicActivity being the View,                                                        *
 *    MicWavRecorderHandler being a model, the middle-man between UI and IO Files related stuff   *
 *    and WavStreamHandler computing the streams (Mic and IO) and sending update signal to        *
 *    the MicActivity/View                                                                    *
 *  _ When starting a recording session if it is not done till completion no file will be kept.   *
 *    Thus RErecording an already existing corpus will erase it regardless if the user            *
 *    complete the second corpus recording session or not !!!!                                    *
 *                                                                                                *
 **************************************************************************************************/

/*****************************************
 * TODO List, what to tackle first:
 *      _ merge + introduce intente and global variable
 */


public class MicActivity extends Activity
{
    /***************************************************
     *                                                 *
     *             VARIABLES DECLARATION               *
     *                                                 *
     ***************************************************/

    /**** Global variables ****/
// TODO declare those \/ elsewhere when merging projects
public static String corpusName="DEBUG"; // get this from intent
public static List<String> commands = new ArrayList<>(); // get this from global variable
// TODO declare those /\ elsewhere when merging projects

    /**** Class variables ****/
    MicWavRecorderHandler mic;
//    private int curCommandListIndex = 0; // iterator used to iterate over the commandList list
public int curCommandListIndex = 0; // iterator used to iterate over the commandList list
    private boolean recordingState = false;

    /**** UI accessors variables ****/
    TextView tv; // Display the currently recording command
    ProgressBar talkingIndicator; // Circle Display bar, indicate when the app is recording
    Button back_btn; // Allow the user to go back to previous recording/Activity

    /**** State machine variable ****/
    private boolean recordingCompleted = false;


    /***************************************************
     *                                                 *
     *              ACTIVITY STATE MACHINE             *
     *                    METHODS                      *
     *                                                 *
     ***************************************************/



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        /********* Standard minimalist UI Initialization *********/
        // Default minimal UI onCreate
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mic_test);

        // Initialize buttons Accessors && actionListerner
        back_btn = (Button) findViewById(R.id.back_button); // Initializing UI accessor
        back_btn.setOnClickListener(new View.OnClickListener() // Setting OnClickListener
        {
            public void onClick(View v)
            {
                previousCommand();
            }
        });
        /*********************************************************/


        /********* Actual Variables initialization *********/
// TODO initialize those \/ elsewhere when merging projects
commands.add("test1");
commands.add("test2");
commands.add("test3");
// TODO initialize those /\ elsewhere when merging projects

        // initialize UI accessors
        tv = (TextView) findViewById(R.id.backgroundTextView);
        talkingIndicator = (ProgressBar) findViewById(R.id.talk_indicator);

        // Destroy previously recorded Corpus with identical name
        destroyCorpus();

        // Initialize MicWavRecorder (will set output Folder and files)
        try
        {
            mic = new MicWavRecorderHandler( 16000, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, this);
                    // create MicWavRecorder according to the audioFormat we need for this Application
                    // <=> Recording @ 16KHz, mono, 16 bits, PCM RIFF Wav
        }
        catch (MicWavRecorderHandlerException e)
        {
            e.printStackTrace();
            System.exit(0);
        }
        mic.start(); // start MicWavRecorder's thread

        // Initialize UI
        updateActivity();
    }

    @Override
    protected void onDestroy()
    {   // check if the recording sessions has been completed
        // otherwise we delete all related files and directory
        if ( !recordingCompleted )
            destroyCorpus();

        // close (and kill threads) MicWavRecorderHandler and its subsidiary WavStreamHandler
        mic.close();
    }



    /***************************************************
     *                                                 *
     *              METHODS DECLARATION                *
     *                                                 *
     ***************************************************/



    public String getCurrentCommandName()
    {   // return string containing text of current command being recorded
        if ( curCommandListIndex >= commands.size() )
            return null; // return null if going OOB
        else
            return commands.get(curCommandListIndex);
    }



    private void previousCommand()
    {   // Iterate to the previous command to be recorded in command's list
        --curCommandListIndex;
        updateActivity();
    }



    public void nextCommand()
    {   // iterate to the next command to be recorded listed in commands's List
        ++curCommandListIndex;
        updateActivity();
//        WavStreamHandler.chibre.notify();

    }


    public void toggleRecordingState()
    {   // Toggle the visibility of the circle progress bar thingy during the recording
        recordingState = !recordingState;
        updateActivity();
    }



    private void updateActivity()
    {   // update UI and state variables
        // DO NOT CHANGE THE CURRENT COMMAND BEING RECORDED !

        // update back_button text and handle enter() & exit()
        switch (curCommandListIndex)
        {
            case -1:
            { // getting back to Menu
                goToPreviousActivity();
                return;
            }
            case 0:
            {  // stage zero, back_button.text <=> cancel
                back_btn.setText(R.string.cancel_recording);
                break;
            }
            default:
            {
                if ( curCommandListIndex >= commands.size() )
                {   // curCommandListIndex is getting out of Bound<=>we reached the end of our commandList
                    recordingCompleted = true;
                    goToNextActivity();
                    return;
                }
                else
                {  // Standard behavior, at least one record previously registered
                    back_btn.setText(R.string.redo_last_recording);
                }
                break;
            }
        }

        // update backgroundTextView
        tv.setText(getCurrentCommandName());

        // update debugTalkingIndicator's state
        if (recordingState)
            talkingIndicator.setVisibility(View.VISIBLE);
        else
            talkingIndicator.setVisibility(View.INVISIBLE);
    }



    /***************************************************
     *                                                 *
     *             DESTROY FOLDER METHOD               *
     *           (UI driven deletion method)           *
     *                                                 *
     ***************************************************/



    private boolean destroyCorpus()
    {   // Method called when user cancels the recording session / application crashes / etc
        // This is not a mic related IO operation so it DOES NOT belong to the "Controller"/MicWavRecorderHandler
        boolean destroySuccess = true;

// TODO DEBUG \/ to be removed and use Global Variables after merging
// get Application's Context
ContextWrapper cw = new ContextWrapper(this.getApplicationContext());
// get Application's data subfolder directory
File baseDir = cw.getDir("data", Context.MODE_PRIVATE);
// create Global Corpus subdirectory
File corpusGlobalDir = new File(baseDir, "Corpus");
if ( !corpusGlobalDir.exists())
return true;

        // get corpus's specific directory
        File corpusDir = new File(corpusGlobalDir, corpusName);
        if (!corpusDir.exists())
            return true;

        // delete its internal files ( *.wav )
        String[] commandFiles = corpusDir.list();
        if (commandFiles.length != 0) // if folder is not empty, destroy its content
            for (String cf : commandFiles)
                destroySuccess = destroySuccess && new File(corpusDir, cf).delete();

        return destroySuccess && corpusDir.delete();
    }




    /***************************************************
     *                                                 *
     *           ACTIVITY TRANSITION METHODS           *
     *              INTENTS MANAGEMENT                 *
     *                                                 *
     ***************************************************/



    private void goToPreviousActivity()
    {   // close & clean mic (File, outputStream, thread, etc)
        mic.close();
//TODO  \/ to replace with correct Load()
Log.i("MicActivity", "goToPreviousActivity");
onDestroy();
System.exit(0);
    }



    private void goToNextActivity()
    {   // close & clean mic (File, outputStreap, thread, etc)
        mic.close();
//TODO  \/ to replace with correct Load()
Log.i("MicActivity", "goToNextActivity");
try { Thread.sleep(500); } catch (InterruptedException e) {e.printStackTrace(); }
if (!recordingCompleted) onDestroy();
System.exit(0);
    }


}
