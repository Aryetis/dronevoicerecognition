package com.dvr.mel.dronevoicerecognition;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class ManageCorpusesActivity extends AppCompatActivity {


    RecyclerView.Adapter userAdapter;
    RecyclerView userCorpusesRecyclerView;
    LinearLayoutManager layoutManager;
    Context context = this;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle("Manage Corpuses");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_corpuses);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        CorpusInfo ci = new CorpusInfo();
        ci.updateFromStaticVariables();

        /*
        * USER RECYCLER VIEW LIST
        * */

        // List adapter of recycler view
        userAdapter = getUserAdapter();
        // Instanciate layoutmanager
        layoutManager = new LinearLayoutManager(context);

        // Get recycler view
        userCorpusesRecyclerView = (RecyclerView) findViewById(R.id.user_corpuses_recyclerview);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(userCorpusesRecyclerView.getContext(),
                layoutManager.getOrientation());

        // Add same divider decorator
        userCorpusesRecyclerView.addItemDecoration(dividerItemDecoration);

        // List adapter
        userCorpusesRecyclerView.setAdapter(userAdapter);

        // Add layout Manager

        userCorpusesRecyclerView.setLayoutManager(layoutManager);

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        String secureName = (String) CorpusInfo.usersCorpora.toArray()[item.getItemId()];
        Corpus corpus = CorpusInfo.corpusMap.get(secureName);
        switch (item.getTitle().toString()){
            case "Remove":
                CorpusInfo.corpusMap.remove(secureName);
                CorpusInfo.referencesCorpora.remove(secureName);
                CorpusInfo.usersCorpora.remove(secureName);
                Toast.makeText(context, "Reference " + corpus.getDisplayName() + " removed.", Toast.LENGTH_SHORT).show();
                break;
            case "Set as reference":
                corpus.setAsReference();
                CorpusInfo.referencesCorpora.add(secureName);
                Toast.makeText(context, "Reference " + corpus.getDisplayName() + " set.", Toast.LENGTH_SHORT).show();
            case "Unset reference":
                corpus.unsetReference();
                CorpusInfo.referencesCorpora.remove(secureName);
                Toast.makeText(context, "Reference " + corpus.getDisplayName() + " unset.", Toast.LENGTH_SHORT).show();
                break;
        }
        this.recreate();
        return super.onContextItemSelected(item);
    }

    public RecyclerView.Adapter getUserAdapter(){
       return new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {



                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.base_rounded_letter_view_item_list, parent, false);

                return new RecyclerView.ViewHolder(view) {
                    @Override
                    public String toString() {
                        return super.toString();
                    }
                };
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                final int finalpos = position;
                final String secureName = (String) CorpusInfo.usersCorpora.toArray()[position];
                final Corpus corpusObject = CorpusInfo.corpusMap.get(secureName);
                ((RoundedLetterView)holder.itemView.findViewById(R.id.rlv_rlv)).setTitleText("C"+position);
                ((TextView)holder.itemView.findViewById(R.id.rlv_text_view)).setText(corpusObject.getDisplayName());
                holder.itemView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                    @Override
                    public void onCreateContextMenu(ContextMenu contextMenu, final View view, ContextMenu.ContextMenuInfo contextMenuInfo) {

                        contextMenu.setHeaderTitle("Select an action");
                        if(CorpusInfo.referencesCorpora.contains(secureName))
                            contextMenu.add(0, finalpos, 0, "Unset reference");
                        else
                            contextMenu.add(0, finalpos, 0, "Set as reference");
                        contextMenu.add(0, finalpos, 0, "Remove");
                        ManageCorpusesActivity.super.onCreateContextMenu(contextMenu, view, contextMenuInfo);
                    }

                });

            }

            @Override
            public int getItemCount() {
                return CorpusInfo.usersCorpora.size();
            }
        };
    }

    public void addNewCorpus(View view) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        builder.setTitle("New Corpus");

        builder.setView(View.inflate(inflater.getContext(), R.layout.new_corpus_form, null));

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                EditText nameEditText =
                        (EditText)((AlertDialog)dialog).findViewById(R.id.newCorpusNameEditText);
                String name = (nameEditText != null ? nameEditText.getText().toString().trim() : "");

                /*EditText descriptionEditText =
                        (EditText)((AlertDialog)dialog).findViewById(R.id.newCorpusDescritpionEditText);
                String description = (nameEditText != null ? descriptionEditText.getText().toString().trim() : "");*/
                if (!name.isEmpty()) {
                    Intent intentToCreateCorpus = new Intent(context, MicActivity.class);
                    String secureName = CorpusInfo.sanitarizeName(name);
                    intentToCreateCorpus.putExtra("name", secureName);
                    intentToCreateCorpus.putExtra("corpus", new Corpus(secureName, name));
                    startActivity(intentToCreateCorpus);
                }
                else
                    Toast.makeText(context, "The name field is empty!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                dialog.cancel();
            }
        });


        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        userAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        userAdapter = getUserAdapter();
        userAdapter.notifyDataSetChanged();
    }
}
