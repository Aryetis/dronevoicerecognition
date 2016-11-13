package com.dvr.mel.dronevoicerecognition;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainMenuActivity extends Activity {

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
}
