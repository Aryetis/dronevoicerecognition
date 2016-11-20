package com.dvr.mel.dronevoicerecognition;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.File;

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

        // populate AppInfo static fields
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
        AppInfo.baseDir = cw.getDir("data", Context.MODE_PRIVATE);
        // <=> /DATA/DATA/com.dvr.mel.dronevoicerecognition/app_data   (yes it prefixes with "app_")

        // check if the file corpusInfoSaved exist (serialized)
        File appInfoSave = new File(AppInfo.baseDir, "appInfoSaved");

        // it doesn't exist
        if ( ! appInfoSave.exists() ) {
            Log.e("launcher", "le fichier serialisé n'existe pas");

            // let's create a sufolder for stocking all of our Corpora
            AppInfo.corpusGlobalDir = new File(AppInfo.baseDir, "Corpus");
            if ( ! AppInfo.corpusGlobalDir.exists())
                AppInfo.corpusGlobalDir.mkdir();
            // <=> /DATA/DATA/com.dvr.mel.dronevoicerecognition/app_data/Corpus

            // .... commands
            AppInfo.commands.add("avance");
            AppInfo.commands.add("recule");
            AppInfo.commands.add("droite");
            AppInfo.commands.add("gauche");
            AppInfo.commands.add("etatdurgence");
            AppInfo.commands.add("tournedroite");
            AppInfo.commands.add("tournegauche");
            AppInfo.commands.add("faisunflip");
            AppInfo.commands.add("arretetoi");

            // save the file
           AppInfo.saveToSerializedFile();
        }

        // it exist
        else {
            AppInfo.loadFromSerializedFile();
        }
    }
}