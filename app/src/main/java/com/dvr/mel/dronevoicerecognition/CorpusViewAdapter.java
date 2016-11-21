package com.dvr.mel.dronevoicerecognition;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Evan on 14/11/2016.
 */

public class CorpusViewAdapter extends BaseAdapter {

    ArrayList<String>  result;
    Context context;
    private static LayoutInflater inflater=null;

    public CorpusViewAdapter(ManageCorporaActivity activity, ArrayList<String> corpusNameList) {
        result=corpusNameList;
        context=activity;

        inflater = ( LayoutInflater )context.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return result.size();
    }

    @Override
    public Object getItem(int i) {
        return result.get(i);
    }

    @Override
    public long getItemId(int i) {
        // TODO MOCKed
        return i;
    }

    public class Holder
    {
        TextView tv;
        RoundedLetterView rlv;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        final int position = i;
        View rowView;
        rowView = inflater.inflate(R.layout.base_rounded_letter_view_item_list, null);
        Holder holder=new Holder();
        holder.tv = (TextView) rowView.findViewById(R.id.rlv_text_view);
        holder.tv.setText(result.get(i));
        holder.rlv = (RoundedLetterView) rowView.findViewById(R.id.rlv_rlv);
        holder.rlv.setTitleText("C"+i);
        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Toast.makeText(context, "You Clicked "+result.get(position), Toast.LENGTH_LONG).show();
            }
        });
        rowView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                // TODO Auto-generated method stub
                Toast.makeText(context, "You Long Clicked "+result.get(position), Toast.LENGTH_LONG).show();
                return true;
            }
        });

        return rowView;
    }
}
