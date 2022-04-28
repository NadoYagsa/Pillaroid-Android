package com.nadoyagsa.pillaroid.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nadoyagsa.pillaroid.R;
import com.nadoyagsa.pillaroid.data.VoiceResultInfo;

import java.util.ArrayList;

public class VoiceResultsListAdapter extends ArrayAdapter<ArrayList<VoiceResultInfo>> {

    Context context;
    private final ArrayList<VoiceResultInfo> voiceResultsList;
    int selectedPosition = -1;

    public VoiceResultsListAdapter(@NonNull Context context, int resource, ArrayList<VoiceResultInfo> voiceResultsList) {
        super(context, resource);
        this.voiceResultsList = voiceResultsList;
        this.context = context;
    }

    @Override
    public int getCount() {
        return voiceResultsList.size();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = inflater.inflate(R.layout.item_voice_results, parent, false);

        TextView tvName = (TextView) convertView.findViewById(R.id.tv_item_voiceresults_name);
        tvName.setText(voiceResultsList.get(position).getMedicineName());

        if (position == selectedPosition) {
            convertView.setBackgroundResource(R.color.main_color);
            tvName.setTextColor(Color.WHITE);
        }

        return convertView;
    }

    public void setSelectedItem(int position) {
        selectedPosition = position;
        notifyDataSetChanged();
    }
}
