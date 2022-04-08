package com.nadoyagsa.pillaroid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import com.nadoyagsa.pillaroid.R;
import com.nadoyagsa.pillaroid.data.AlarmInfo;

import java.util.ArrayList;

public class AlarmRecyclerAdapter extends RecyclerView.Adapter<AlarmRecyclerAdapter.AlarmViewHolder> {
    private Context context;
    private ArrayList<AlarmInfo> alarmList;

    public AlarmRecyclerAdapter(ArrayList<AlarmInfo> alarmList) {
        this.alarmList = alarmList;
    }

    @NonNull
    @Override
    public AlarmRecyclerAdapter.AlarmViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.item_alarm, parent, false);
        AlarmRecyclerAdapter.AlarmViewHolder viewHolder = new AlarmRecyclerAdapter.AlarmViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmRecyclerAdapter.AlarmViewHolder holder, int position) {
        holder.tvAlarmName.setText(alarmList.get(position).getMedicineName());
        holder.tvAlarmDetails.setText(alarmList.get(position).getDetails());
    }

    @Override
    public int getItemCount() { return alarmList.size(); }

    public class AlarmViewHolder extends RecyclerView.ViewHolder {
        AppCompatButton btDelete;
        TextView tvAlarmName, tvAlarmDetails;

        AlarmViewHolder(final View itemView) {
            super(itemView);

            tvAlarmName = itemView.findViewById(R.id.tv_item_alarm_name);
            tvAlarmDetails = itemView.findViewById(R.id.tv_item_alarm_details);
            btDelete = itemView.findViewById(R.id.bt_item_alarm_delete);

            btDelete.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    //TODO: 약 복용 알림 삭제
                }
            });
        }
    }
}
