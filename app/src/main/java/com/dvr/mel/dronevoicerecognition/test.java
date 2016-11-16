package com.dvr.mel.dronevoicerecognition;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class test extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);


        Intent intent = new Intent(this, finalCorpus.class);

        Bundle b = new Bundle();
        b.putString("corpusName", "test");

        intent.putExtras(b);

        startActivity(intent);

    }
}
