package com.nadoyagsa.pillaroid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nadoyagsa.pillaroid.R;
import com.nadoyagsa.pillaroid.data.VoiceResultInfo;

import java.util.ArrayList;

public class VoiceResultsRecyclerAdapter extends RecyclerView.Adapter<VoiceResultsRecyclerAdapter.VoiceResultsViewHolder> {
    private final ArrayList<VoiceResultInfo> voiceResultsList;

    public VoiceResultsRecyclerAdapter(ArrayList<VoiceResultInfo> voiceResultsList) {
        this.voiceResultsList = voiceResultsList;
    }

    @NonNull
    @Override
    public VoiceResultsRecyclerAdapter.VoiceResultsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.item_voice_results, parent, false);
        VoiceResultsRecyclerAdapter.VoiceResultsViewHolder viewHolder = new VoiceResultsRecyclerAdapter.VoiceResultsViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull VoiceResultsRecyclerAdapter.VoiceResultsViewHolder holder, int position) {
        holder.tvName.setText(voiceResultsList.get(position).getMedicineName());
    }

    @Override
    public int getItemCount() {
        return voiceResultsList.size();
    }

    public static class VoiceResultsViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;

        public VoiceResultsViewHolder(@NonNull View itemView) {
            super(itemView);

            this.tvName = itemView.findViewById(R.id.tv_item_voiceresults_name);
        }
    }
}
