package com.dvr.mel.dronevoicerecognition;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.File;

public class finalCorpus extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final_corpus);


        // get all the parameters
        Bundle b = getIntent().getExtras();
        String corpusName = b.getString("corpusName");
        //String corpusName = stringFromJNI();


        // change the first textView
        String textForLabel = "corpus " + corpusName + " enregistré";
        TextView label = (TextView) findViewById(R.id.label);
        label.setText(textForLabel);

        // check if one or more corpus are set as references
        // no - put a message that there is no references yet to compare with
        TextView middleLabel = (TextView) findViewById(R.id.labelRecognition);

        //CorpusInfo.referencesCorpora.add("M01");    // test
        if (CorpusInfo.referencesCorpora.isEmpty()) {
            middleLabel = (TextView) findViewById(R.id.labelRecognition);
            middleLabel.setText("Aucune références à été définie");
        }

        // yes - run the recognition code true all the references and display the succes percent
        else {
            float percent = computeRecognitionRatio(CorpusInfo.corpusGlobalDir.getAbsolutePath(), "M01", "M02");
            middleLabel.setText(Float.toString(percent));
        }

    }

    public void updateProgressLabel(String newText) {
        TextView progressLabel = (TextView) findViewById(R.id.progressLabel);
        progressLabel.setText(newText);
    }

    public native float computeRecognitionRatio(String pathToSDCard, String reference, String hypothese);
    static {
        System.loadLibrary("native-lib");
    }
}
