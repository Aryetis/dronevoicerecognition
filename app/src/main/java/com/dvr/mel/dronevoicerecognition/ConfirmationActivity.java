package com.dvr.mel.dronevoicerecognition;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import org.w3c.dom.Text;

public class ConfirmationActivity extends AppCompatActivity {
    Bundle b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation);

        b = getIntent().getExtras();

        TextView finalLabel = (TextView) findViewById(R.id.finalLabel);
        finalLabel.setText("Le corpus " + b.getString("name") + " à bien été enregistré");

        saveCorpus();
    }

    private void saveCorpus() {
        AppInfo.addCorpus(b.getString("name"), (Corpus) b.getSerializable("corpus"));
        AppInfo.saveToSerializedFile();
    }
}
