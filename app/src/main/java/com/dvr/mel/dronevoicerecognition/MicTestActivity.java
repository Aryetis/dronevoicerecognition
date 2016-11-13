package com.dvr.mel.dronevoicerecognition;

import android.app.Activity;
import android.media.AudioFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;


/**********************************************************************
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!  *
 * BY NOTATION, EVERY CODE THAT IS NOT ALIGNED / @ FIRST COLUMN       *
 * IS FOR DEBUG PURPOSES ONLY AND THUS MUST BE REMOVED BEFORE MERGING *
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!  *
 **********************************************************************/



public class MicTestActivity extends Activity
{
    /***************************************************
     *                                                 *
     *             VARIABLES DECLARATION               *
     *                                                 *
     ***************************************************/

    // Global variables
// TODO declare those \/ elsewhere when merging projects
String corpusName="DEBUG";
String appFolderName="/DATA/APP/com.dvr.mel.dronevoicerecognition/";
List<String> commandList = new ArrayList<>();
// TODO declare those /\ elsewhere when merging projects

    // Class variables
    MicWavRecorder mic;
    int curCommandListIndex = 0; // iterator used to iterate over the commandList list

    // UI accessors variables
    TextView tv; // Display the currently recording command
TextView debug_talking_indicator_tv; // DEBUG : used to display if the user is talking or not
    Button back_btn; // Allow the user to go back to previous recording/Activity
Button debug_btn; // DEBUG : used to force validation of the current command



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
                recordPreviousCommand();
            }
        });
debug_btn = (Button) findViewById(R.id.debug_button); // Initializing UI accessor
debug_btn.setOnClickListener(new View.OnClickListener() // Setting OnClickListener
{
    public void onClick(View v)
    {
        recordNextCommand();
    }
});
        /*********************************************************/


        /********* Actual Variables initialization *********/
// TODO initialize those \/ elsewhere when merging projects
commandList.add("test1");
commandList.add("test2");
commandList.add("test3");
commandList.add("test4");
commandList.add("test5");
// TODO initialize those /\ elsewhere when merging projects

        // initialize UI accessors
        tv = (TextView) findViewById(R.id.backgroundTextView);
debug_talking_indicator_tv = (TextView) findViewById(R.id.debugTalkIndicator);

        // Initialize MicWavRecorder
        mic = new MicWavRecorder(1.F, 4.F, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mic.start(); // start MicWavRecorder thread

        // Initialize UI
        updateUI();
    }



    /***************************************************
     *                                                 *
     *              METHODS DECLARATION                *
     *                                                 *
     ***************************************************/



    private void recordPreviousCommand()
    {
        // TODO delete existing file record, no leftovers, it's all or nothing

        curCommandListIndex--;
        updateUI();
    }



    private void recordNextCommand()
    {
        curCommandListIndex++;
        updateUI();
    }



    private void updateUI()
    {
        // update back_button text and handle enter() & exit()
        switch (curCommandListIndex)
        {
            case -1:
            { // getting back to Menu
                goToPreviousActivity();
                break;
            }
            case 0:
            {  // stage zero, back_button.text <=> cancel
                back_btn.setText("Cancel Recording");
                break;
            }
            default:
            {
                if ( curCommandListIndex > commandList.size() )
                {
                    goToNextActivity();
                }
                else
                {  // Standard behavior, at least one record previously registered
                    back_btn.setText("Redo last recording");
                }
                break;
            }
        }

        // update backgroundTextView
        tv.setText(commandList.get(curCommandListIndex));

// set debugTalkingIndicator
setDebugTalkIndicator(false);
    }



    private void goToPreviousActivity()
    {
        // close & clean mic (File, outputStream, thread, etc)
        mic.close();
//TODO  \/ to replace with correct Load()
Log.i("MicTestActivity", "goToPreviousActivity");
System.exit(0);
    }



    private void goToNextActivity()
    {
        // close & clean mic (File, outputStreap, thread, etc)
        mic.close();
//TODO  \/ to replace with correct Load()
Log.i("MicTestActivity", "goToNextActivity");
System.exit(0);
    }



/***************************************************
 *           \                      /              *
 *               \DEBUG SECTION/                   *
 *                     \/                          *
 ***************************************************/

public void setDebugTalkIndicator(boolean b)
{
debug_talking_indicator_tv.setText( (b) ? "TALKING" : "SILENCE" );
}

}
