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
import android.util.Log;
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

        // build tree directory if needed
        // get the base dir for all date linked to our application
        CorpusInfo.baseDir = cw.getDir("data", Context.MODE_PRIVATE);
        // <=> /DATA/DATA/com.dvr.mel.dronevoicerecognition/app_data   (yes it prefixes with "app_")

        // check if the file corpusInfoSaved exist (serialized)
        File corpusInfoSave = new File(CorpusInfo.baseDir, "corpusInfoSaved");

        // it doesn't exist
        if ( ! corpusInfoSave.exists() ) {
            Log.e("launcher", "le fichier serialisé n'existe pas");

            // let's create a sufolder for stocking all of our Corpora
            CorpusInfo.corpusGlobalDir = new File(CorpusInfo.baseDir, "Corpus");
            if ( ! CorpusInfo.corpusGlobalDir.exists())
                CorpusInfo.corpusGlobalDir.mkdir();
            // <=> /DATA/DATA/com.dvr.mel.dronevoicerecognition/app_data/Corpus

            // .... commands
            CorpusInfo.commands.add("avance");
            CorpusInfo.commands.add("recule");
            CorpusInfo.commands.add("droite");
            CorpusInfo.commands.add("gauche");
            CorpusInfo.commands.add("etatdurgence");
            CorpusInfo.commands.add("tournedroite");
            CorpusInfo.commands.add("tournegauche");
            CorpusInfo.commands.add("faisunflip");
            CorpusInfo.commands.add("arretetoi");

            // save the file
            try {
                CorpusInfo ci = new CorpusInfo();
                ci.updateFromStaticVariables();

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
            Log.e("launcher", "le fichier serialisé existe");
            try {
                CorpusInfo ci = new CorpusInfo();

                FileInputStream fileIn = new FileInputStream(corpusInfoSave.getAbsolutePath());
                ObjectInputStream in = new ObjectInputStream(fileIn);

                ci = (CorpusInfo) in.readObject();
                ci.updateToStaticVariables();

                in.close();
                fileIn.close();

                Log.e("launcher", "corpusGlobalDir path : " + CorpusInfo.corpusGlobalDir.getAbsolutePath());

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