package com.dvr.mel.dronevoicerecognition;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class FinalCorpusActivity extends AppCompatActivity {
    public Bundle b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final_corpus);


        // get all the parameters
        b = getIntent().getExtras();
        String corpusName = b.getString("name");

        // change the first textView with the name of the corpus currently being tested
        String textForLabel = "corpus " + corpusName + " enregistré";
        TextView label = (TextView) findViewById(R.id.label);
        label.setText(textForLabel);

        // check if one or more corpus are set as references
        // The first corpus is automatically set as reference
        TextView middleLabel = (TextView) findViewById(R.id.labelRecognition);

        if (AppInfo.referencesCorpora.isEmpty()) {
            middleLabel = (TextView) findViewById(R.id.labelRecognition);
            middleLabel.setText("Aucune références à été définie");

            AppInfo.referencesCorpora.add(b.getString("name"));
            AppInfo.addCorpus(b.getString("name"), (Corpus) b.getSerializable("corpus"));

            AppInfo.saveToSerializedFile();
        }

        // yes - run the recognition code true all the references and display the success percent
        else {
            float percent = computeRecognitionRatio(
                AppInfo.corpusGlobalDir.getAbsolutePath(),
                new ArrayList<String>(AppInfo.referencesCorpora),
                b.getString("name"));

            middleLabel.setText(Float.toString(percent));
        }
    }

    /**
     * The user pressed the red cross.
     * Delete the files and directory linked to the corpus
     * @param view
     */
    public void corpusFailHandler(View view) {
        AppInfo.clean(b.getString("name"));

        AppInfo.saveToSerializedFile();

        startActivity(new Intent(this, ManageCorporaActivity.class));
    }

    /**
     * The user presses the green cross.
     * Update the static variables linked to the AppInfo class. usersCorpora and corpusMap.
     * Update the serialized file of the class into the memory. and go back to the ManageCorpus
     * Activity
     * @param view
     */
    public void corpusPassHandler(View view) {
        // Mettre à jours la class AppInfo.
        AppInfo.addCorpus(b.getString("name"), (Corpus) b.getSerializable("corpus"));

        AppInfo.saveToSerializedFile();

        startActivity(new Intent(this, ManageCorporaActivity.class));
    }

    /**
     * Function called from C++ code to update the message under the progress bar. only design and
     * aesthetic
     * @param newText
     */
    public void updateProgressLabel(String newText) {
        TextView progressLabel = (TextView) findViewById(R.id.progressLabel);
        progressLabel.setText(newText);
    }

    /**
     * Will calculate the recognition rate reached by the corpus newly created. The algorithme allow
     * to use multiple references in order to improve the results.
     * @param pathToSDCard
     * @param references
     * @param hypothese     The corpus recently created
     * @return
     */
    public native float computeRecognitionRatio(String pathToSDCard, List<String> references, String hypothese);

    /**
     * Load the C++ library into the application.
     * Allow the use of all functions which can be find in the .cpp files.
     */
    static {
        System.loadLibrary("native-lib");
    }


}
