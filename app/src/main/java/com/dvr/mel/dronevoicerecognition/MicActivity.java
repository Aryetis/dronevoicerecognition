package com.dvr.mel.dronevoicerecognition;

// UI imports
import android.content.Intent;
import android.media.AudioFormat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
// Corpus management imports
import java.io.File;



/**************************************************************************************************
 *  MicActivity in a nutshell:                                                                    *
 *      _ get and pass a Bundle of informations to FinalCorpusActivity                            *
 *      _ Use MicWavRecorderHandler (& WavStreamHandler) to record                                *
 *        a list of Wav audio files according to corpusList variable located in AppInfo           *
 *      _ Handle deletion/overwriting of files and folder in case of record session cancellation  *
 *        / app crashes / etc                                                                     *
 *                                                                                                *
 *  Sidenotes :                                                                                   *
 *  _ If it helps, see Mic related Activities as MVC designed activities,                         *
 *    with MicActivity being the View,                                                            *
 *    MicWavRecorderHandler being a model, the middle-man between UI and IO Files related stuff   *
 *    and WavStreamHandler computing the streams (Mic and IO) and sending update signal to        *
 *    the MicActivity/View                                                                        *
 *  _ When starting a recording session if it is not done till completion no file will be kept.   *
 *    Thus RErecording an already existing corpus will erase it regardless if the user            *
 *    complete the second corpus recording session or not !!!!                                    *
 *                                                                                                *
 *   Author : https://github.com/Aryetis                                                          *
 **************************************************************************************************/



public class MicActivity extends AppCompatActivity
{
    /***************************************************
     *                                                 *
     *             VARIABLES DECLARATION               *
     *                                                 *
     ***************************************************/

    /**** Global variables ****/
    public static String corpusName; // name of the Corpus to be created
                                     // (acquire it from Intent created in ManageCorpusActivity)

    /**** Class variables ****/
    MicWavRecorderHandler mic;
    private int curCommandListIndex = 0; // iterator used to iterate over the commandList list
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
        setTitle("Corpus Recording Session");
        super.onCreate(savedInstanceState);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_mic);


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

        // Global variables / Intent's variables initialisation
        corpusName = getIntent().getExtras().getString("name", "DefaultCorpusName");

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

        // Calling default onDestroy()
        super.onDestroy();
    }


    @Override
    protected void onStop()
    {
        // Calling default onStop()
        super.onStop();
    }



    /***************************************************
     *                                                 *
     *              METHODS DECLARATION                *
     *                                                 *
     ***************************************************/



    public String getCurrentCommandName()
    {   // return string containing text of current command being recorded
        if ( curCommandListIndex >= AppInfo.commands.size() )
            return null; // return null if going OOB
        else
            return AppInfo.commands.get(curCommandListIndex);
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
                if ( curCommandListIndex >= AppInfo.commands.size() )
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

        // get corpus's specific directory
        File corpusDir = new File( AppInfo.baseDir, corpusName);
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
    {   // Close & clean mic (File, outputStream, thread, etc)
        mic.close();

        // Load MainMenuActivity
        Intent intent = new Intent(this, MainMenuActivity.class);
        startActivity(intent);
    }



    private void goToNextActivity()
    {   // Close & clean mic (File, outputStream, thread, etc)
        mic.close();

        /**** Load FinalCorpusActivity ****/
        // Create intent
        Intent intent = new Intent(this, FinalCorpusActivity.class);
        // Get Bundle from MainMenuActivity and pass it over
        Bundle b = getIntent().getExtras();
        intent.putExtras(b);
        // Launch activity
        startActivity(intent);
    }
}
