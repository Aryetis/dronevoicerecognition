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

public class ManageCorpusesActivity extends AppCompatActivity {


    RecyclerView.Adapter adapter;
    RecyclerView.Adapter userAdapter;
    RecyclerView staticCorpusesRecyclerView;
    RecyclerView userCorpusesRecyclerView;
    LinearLayoutManager layoutManager;
    Context context = this;
    // TODO Link with real data
    final ArrayList<String> staticMockList = new ArrayList<String>();
    // TODO Link with real data
    final ArrayList<String> userMockList = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle("Manage Corpuses");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_corpuses);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

       /*
        * STATIC RECYCLER VIEW LIST
        * */
        // Layout manager of recycler view
        layoutManager = new LinearLayoutManager(context);

        // List adapter of recycler view
        adapter = new RecyclerView.Adapter() {
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
                ((RoundedLetterView)holder.itemView.findViewById(R.id.rlv_rlv)).setTitleText("C"+position);
                ((TextView)holder.itemView.findViewById(R.id.rlv_text_view)).setText(staticMockList.get(position));
                holder.itemView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                    @Override
                    public void onCreateContextMenu(ContextMenu contextMenu, final View view, ContextMenu.ContextMenuInfo contextMenuInfo) {

                        contextMenu.setHeaderTitle("Select an action");
                        contextMenu.add(0, finalpos, 0, "Set as reference");
                        ManageCorpusesActivity.super.onCreateContextMenu(contextMenu, view, contextMenuInfo);
                    }

                });

            }

            @Override
            public int getItemCount() {
                return staticMockList.size();
            }
        };

        // Get recycler view
        staticCorpusesRecyclerView = (RecyclerView) findViewById(R.id.corpuses_recyclerview);

        // Add divider decorator
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(staticCorpusesRecyclerView.getContext(),
                layoutManager.getOrientation());
        staticCorpusesRecyclerView.addItemDecoration(dividerItemDecoration);

        // List adapter
        staticCorpusesRecyclerView.setAdapter(adapter);

        // Add layout Manager
        staticCorpusesRecyclerView.setLayoutManager(layoutManager);

        /*
        * USER RECYCLER VIEW LIST
        * */

        // List adapter of recycler view
        userAdapter = new RecyclerView.Adapter() {
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
                ((RoundedLetterView)holder.itemView.findViewById(R.id.rlv_rlv)).setTitleText("C"+position);
                ((TextView)holder.itemView.findViewById(R.id.rlv_text_view)).setText(userMockList.get(position));
                holder.itemView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                    @Override
                    public void onCreateContextMenu(ContextMenu contextMenu, final View view, ContextMenu.ContextMenuInfo contextMenuInfo) {

                        contextMenu.setHeaderTitle("Select an action");
                        contextMenu.add(0, finalpos, 0, "Set as reference");
                        contextMenu.add(0, finalpos, 0, "Remove");
                        ManageCorpusesActivity.super.onCreateContextMenu(contextMenu, view, contextMenuInfo);
                    }

                });

            }

            @Override
            public int getItemCount() {
                return userMockList.size();
            }
        };


        // Get recycler view
        userCorpusesRecyclerView = (RecyclerView) findViewById(R.id.user_corpuses_recyclerview);

        // Add same divider decorator
        userCorpusesRecyclerView.addItemDecoration(dividerItemDecoration);

        // List adapter
        userCorpusesRecyclerView.setAdapter(userAdapter);

        // Add layout Manager
        layoutManager = new LinearLayoutManager(context);
        userCorpusesRecyclerView.setLayoutManager(layoutManager);

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getTitle().toString()){
            case "Remove":
                userMockList.remove(item.getItemId());
                userAdapter.notifyDataSetChanged();
                break;
            case "Set as reference":
                // TODO Set as reference
                break;
        }
        Toast.makeText(context, "Removed " + item.getItemId(), Toast.LENGTH_SHORT).show();
        return super.onContextItemSelected(item);
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
                    String secureName = AppInfo.sanitarizeName(name);
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
}
