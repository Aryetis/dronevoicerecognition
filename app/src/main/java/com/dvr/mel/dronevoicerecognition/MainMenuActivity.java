package com.dvr.mel.dronevoicerecognition;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle("Main Menu");

        super.onCreate(savedInstanceState);
        //Icon

        if(getActionBar()!=null) {
            getActionBar().setHomeButtonEnabled(true);
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setContentView(R.layout.activity_main_menu);


    }

    public void startManageCorporaActivity(View view) {
        Intent intent = new Intent(this, ManageCorporaActivity.class);
        startActivity(intent);
    }

    public void startMicCalibrationActivity(View view) {
        Intent intent = new Intent(this, MicCalibrationActivity.class);
        startActivity(intent);
    }
}
