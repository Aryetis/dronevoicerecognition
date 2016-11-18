package com.dvr.mel.dronevoicerecognition;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class LauncherActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle("Welcome!");

        super.onCreate(savedInstanceState);

        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //Set content view
        setContentView(R.layout.activity_launcher);

        //Support remove action bar
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        //Custom font
        TextView tx = (TextView) findViewById(R.id.launcherTitle);
        Typeface custom_font = Typeface.createFromAsset(getAssets(), "fonts/letraHipster.ttf");
        tx.setTypeface(custom_font);

        // populate CorpusInfo static fields
        populateCorpusInfo();
    }

    public void startMainMenuActivity(View view) {
        Intent intent = new Intent(this, MainMenuActivity.class);
        startActivity(intent);
    }

    private void populateCorpusInfo() {
        // getApplication's context
        ContextWrapper cw = new ContextWrapper(this.getApplicationContext());

        // set some references with CorpusInfo
        File baseDir = CorpusInfo.baseDir;
        File corpusGlobalDir = CorpusInfo.corpusGlobalDir;
        List<String> commands = CorpusInfo.commands;

        // build tree directory if needed
        // get the base dir for all date linked to our application
        baseDir = cw.getDir("data", Context.MODE_PRIVATE);
        // <=> /DATA/DATA/com.dvr.mel.dronevoicerecognition/app_data   (yes it prefixes with "app_")

        // check if the file corpusInfoSaved exist (serialized)
        File corpusInfoSave = new File(baseDir, "corpusInfoSaved");

        // it doesn't exist
        if (!corpusInfoSave.exists()) {

            // let's create a sufolder for stocking all of our Corpora
            corpusGlobalDir = new File(baseDir, "Corpus");
            if (!corpusGlobalDir.exists())
                corpusGlobalDir.mkdir();
            // <=> /DATA/DATA/com.dvr.mel.dronevoicerecognition/app_data/Corpus

            // .... commands
            commands.add("avance");
            commands.add("recule");
            commands.add("droite");
            commands.add("gauche");
            commands.add("etatdurgence");
            commands.add("tournedroite");
            commands.add("tournegauche");
            commands.add("faisunflip");
            commands.add("arretetoi");

            // save the file
            try {
                CorpusInfo ci = new CorpusInfo();

                FileOutputStream fileOut = new FileOutputStream(corpusInfoSave.getAbsolutePath());
                ObjectOutputStream out = new ObjectOutputStream(fileOut);

                out.writeObject(ci);

                out.close();
                fileOut.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // it exist
        else {
            try {
                CorpusInfo ci = new CorpusInfo();

                FileInputStream fileIn = new FileInputStream(corpusInfoSave.getAbsolutePath());
                ObjectInputStream in = new ObjectInputStream(fileIn);

                ci = (CorpusInfo) in.readObject();

                in.close();
                fileIn.close();


                // update static variables
                CorpusInfo.referencesCorpora = ci._referencesCorpora;
                CorpusInfo.usersCorpora = ci._usersCorpora;
                CorpusInfo.baseDir = ci._baseDir;
                CorpusInfo.corpusGlobalDir = ci._corpusGlobalDir;
                CorpusInfo.corpusMap = ci._corpusMap;
                CorpusInfo.commands = ci._commands;

            } catch (FileNotFoundException e) {
                e.printStackTrace();

            } catch (IOException e) {
                e.printStackTrace();

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}