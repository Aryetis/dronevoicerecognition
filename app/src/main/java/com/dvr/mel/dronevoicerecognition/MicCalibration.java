package com.dvr.mel.dronevoicerecognition;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;


/**************************************************************************************************
 *  MicCalibration in a nutshell:                                                                 *
 *      _ Set/Get AppInfo.SENSITIVITY and AppInfo.BUFFER_SIZE_MULTIPLICATOR                       *
 *                                                                                                *
 *   Author : https://github.com/Aryetis                                                          *
 **************************************************************************************************/

public class MicCalibration extends Activity
{
    /**** UI accessors variables ****/
    // SeekBar minimal values are 0, rule enforced by android. Thus we add 10 to get useful values
    SeekBar sensitivity_bar; // Min value = 5 ; Max value = 45 => Actual useful value: 45 - SeekBar.value
                             // Need to "inverse" value, so the higher the SeekBar is the more sensible the mic is
    SeekBar recording_window_bar; // Min value = 2 ; Max value = 20 => Actual useful value: SeekBar.value + 2
    Button reset_button;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        /**** Default minimal UI onCreate ****/
        setTitle("Mic Calibration");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mic_calibration);

        /**** Initialize buttons Accessors && actionListerner ****/
        // sensitivity_bar
        sensitivity_bar = (SeekBar) findViewById(R.id.sensitivity_bar);
        sensitivity_bar.setProgress( 45-AppInfo.SENSITIVITY ); // get Actual value and translate it to UI value
        Log.e("MicCalibration", "Initial sensitivity_bar : "+AppInfo.SENSITIVITY);
        sensitivity_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b)
            { /* Nothing to do */ }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            { /* Nothing to do */ }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            { AppInfo.SENSITIVITY = 45-sensitivity_bar.getProgress(); Log.e("MicCalibration", "sensitivity_bar set to : "+AppInfo.SENSITIVITY); }
        });
        // recording_window_bar
        recording_window_bar = (SeekBar) findViewById(R.id.recording_window_bar);
        recording_window_bar.setProgress( AppInfo.BUFFER_SIZE_MULTIPLICATOR+2 ); // get Actual value and translate it to UI value
        Log.e("MicCalibration", "Initial recording_window_bar : "+AppInfo.SENSITIVITY);
        recording_window_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b)
            { /* Nothing to do */ }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            { /* Nothing to do */ }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            { AppInfo.BUFFER_SIZE_MULTIPLICATOR = recording_window_bar.getProgress()-2; Log.e("MicCalibration", "recording_window_bar set to : "+AppInfo.SENSITIVITY);  }
        });
        // reset_button
        reset_button = (Button) findViewById(R.id.reset_button);
        reset_button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                sensitivity_bar.setProgress(35); AppInfo.SENSITIVITY = 10;
                recording_window_bar.setProgress(8); AppInfo.BUFFER_SIZE_MULTIPLICATOR = 10;
            }
        });
    }

    @Override
    protected void onDestroy()
    {
        // Save settings/serialize file
        AppInfo.saveToSerializedFile();

        // Calling default onDestroy()
        super.onDestroy();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
    }

}
