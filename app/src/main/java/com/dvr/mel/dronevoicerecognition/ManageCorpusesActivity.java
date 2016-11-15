package com.dvr.mel.dronevoicerecognition;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class ManageCorpusesActivity extends AppCompatActivity {


    RecyclerView.Adapter adapter;
    RecyclerView recyclerView;
    Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle("Manage Corpuses");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_corpuses);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // TODO Link with real data
        final ArrayList<String> mockList = new ArrayList<String>();
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
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Todo Mocked
                        Toast.makeText(context, "You Clicked "+mockList.get(finalpos), Toast.LENGTH_LONG).show();
                    }
                });
                holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        // Todo Mocked
                        Toast.makeText(context, "You Long Clicked "+mockList.get(finalpos), Toast.LENGTH_LONG).show();
                        return false;
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


        mockList.add("Corpus de Référence");
        mockList.add("Corpus Matthias");
        mockList.add("Corpus Leo");
        mockList.add("Corpus Evan");

        adapter.notifyDataSetChanged();

    }
}
