package com.dvr.mel.dronevoicerecognition;

import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class ManageCorpusesActivity extends AppCompatActivity {


    RecyclerView.Adapter adapter;
    RecyclerView recyclerView;
    Context context = this;
    // TODO Link with real data
    final ArrayList<String> mockList = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle("Manage Corpuses");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_corpuses);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        // Layout manager of recycler view
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);

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
                ((TextView)holder.itemView.findViewById(R.id.rlv_text_view)).setText(mockList.get(position));
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
                return mockList.size();
            }
        };


        // Get recycler view
        recyclerView = (RecyclerView) findViewById(R.id.corpuses_recyclerview);




        // Add divider decorator
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);

        // List adapter
        recyclerView.setAdapter(adapter);

        // Add layout Manager
        recyclerView.setLayoutManager(layoutManager);

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getTitle().toString()){
            case "Remove":
                mockList.remove(item.getItemId());
                adapter.notifyDataSetChanged();
                break;
            case "Set as reference":
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
                if (!name.isEmpty()) {
                    mockList.add(name);
                    adapter.notifyDataSetChanged();
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
