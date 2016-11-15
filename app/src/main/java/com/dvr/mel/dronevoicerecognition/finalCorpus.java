package com.dvr.mel.dronevoicerecognition;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import org.w3c.dom.Text;

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
        if (CorpusInfo.referencesCorpora.isEmpty()) {
            TextView middleLabel = (TextView) findViewById(R.id.labelRecognition);
            middleLabel.setText("Aucun corpus de référence n'as été défini");
        }

        // yes - run the recognition code true all the references and display the succes percent
        else {

        }
    }

    public native String stringFromJNI();
    public native String reconnaissance(String reference, String hypothese, String unknowWord);
    public native float computeRecognitionRatio(String reference, String hypothese);
    static {
        System.loadLibrary("native-lib");
    }
}
