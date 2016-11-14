package com.dvr.mel.dronevoicerecognition;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ListViewCompat;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class ManageCorpusesActivity extends AppCompatActivity {


    ArrayAdapter<String> adapter;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle("Manage Corpuses");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_corpuses);
        if(getActionBar() != null) getActionBar().setDisplayHomeAsUpEnabled(true);

        // TODO Link with real data
        ArrayList<String> mockList = new ArrayList<String>();

        adapter = new ArrayAdapter<>(this, R.layout.corpus_list_item_textview, mockList);

        listView = (ListView) findViewById(R.id.corpuses_listview);
        listView.setAdapter(adapter);

        mockList.add("Corpus de Référence");
        mockList.add("Corpus Matthias");
        mockList.add("Corpus Leo");
        mockList.add("Corpus Evan");

        adapter.notifyDataSetChanged();

    }
}
