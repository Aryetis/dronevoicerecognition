package com.dvr.mel.dronevoicerecognition;

import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;

public class ManageCorpusesActivity extends Activity {




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_corpuses);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

}
