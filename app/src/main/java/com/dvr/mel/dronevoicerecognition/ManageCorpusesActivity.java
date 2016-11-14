package com.dvr.mel.dronevoicerecognition;

import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;

public class ManageCorpusesActivity extends AppCompatActivity {




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle("Manage Corpuses");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_corpuses);
        if(getActionBar() != null) getActionBar().setDisplayHomeAsUpEnabled(true);
    }
}
