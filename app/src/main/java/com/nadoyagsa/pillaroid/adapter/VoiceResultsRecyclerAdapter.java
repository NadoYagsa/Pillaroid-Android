package com.nadoyagsa.pillaroid.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nadoyagsa.pillaroid.MedicineResultActivity;
import com.nadoyagsa.pillaroid.R;
import com.nadoyagsa.pillaroid.VoiceResultsActivity;
import com.nadoyagsa.pillaroid.data.VoiceResultInfo;

import java.util.ArrayList;

public class VoiceResultsRecyclerAdapter extends RecyclerView.Adapter<VoiceResultsRecyclerAdapter.VoiceResultsViewHolder> {
    private Context context;
    private final ArrayList<VoiceResultInfo> voiceResultsList;

    public VoiceResultsRecyclerAdapter(ArrayList<VoiceResultInfo> voiceResultsList) {
        this.voiceResultsList = voiceResultsList;
    }

    @NonNull
    @Override
    public VoiceResultsRecyclerAdapter.VoiceResultsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
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

    public class VoiceResultsViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;

        public VoiceResultsViewHolder(@NonNull View itemView) {
            super(itemView);

            this.tvName = itemView.findViewById(R.id.tv_item_voiceresults_name);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    //TODO: 약의 세부 내용 보여줌
                    context.startActivity(new Intent(context, MedicineResultActivity.class));
                }
            });
        }
    }
}
