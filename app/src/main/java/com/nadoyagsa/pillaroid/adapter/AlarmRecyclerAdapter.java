package com.nadoyagsa.pillaroid.adapter;

import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;
import static com.nadoyagsa.pillaroid.MainActivity.tts;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.speech.tts.TextToSpeech;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.RecyclerView;

import com.nadoyagsa.pillaroid.MedicineResultActivity;
import com.nadoyagsa.pillaroid.PillaroidAPIImplementation;
import com.nadoyagsa.pillaroid.R;
import com.nadoyagsa.pillaroid.SharedPrefManager;
import com.nadoyagsa.pillaroid.data.AlarmInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlarmRecyclerAdapter extends RecyclerView.Adapter<AlarmRecyclerAdapter.AlarmViewHolder> implements ItemTouchHelperListener {
    private Context context;
    private ArrayList<AlarmInfo> alarmList;

    private long delay = 0;
    private Integer currentClickedPos = null;

    public AlarmRecyclerAdapter(ArrayList<AlarmInfo> alarmList) { this.alarmList = alarmList; }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.item_alarm, parent, false);
        AlarmViewHolder viewHolder = new AlarmViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        holder.tvAlarmName.setText(alarmList.get(position).getName());
        holder.tvAlarmDetails.setText(alarmList.get(position).getDosage());
    }

    @Override
    public int getItemCount() { return alarmList.size(); }

    @Override
    public void onItemSwipe(int position) { // 알림 삭제
        tts.speak("Are you sure you want to delete notifications from ".concat(alarmList.get(position).getName()).concat("?"), QUEUE_FLUSH, null, null);

        View deleteAlarmDialogView = View.inflate(context, R.layout.dialog_delete, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(deleteAlarmDialogView);
        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        alertDialog.show();

        WindowManager.LayoutParams params = alertDialog.getWindow().getAttributes();
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        int width = size.x;
        params.width = (int) (width*0.75);
        alertDialog.getWindow().setAttributes(params);

        AppCompatImageButton ibtDeleteAlarm = deleteAlarmDialogView.findViewById(R.id.ibt_dialog_delete);
        // 휴지통 이미지 버튼을 꾹 누르면 삭제, 클릭하면 취소
        ibtDeleteAlarm.setOnLongClickListener(view -> {
            PillaroidAPIImplementation.getApiService().deleteAlarm(SharedPrefManager.read("token", null), alarmList.get(position).getAlarmIdx()).enqueue(new Callback<String>() {
                @Override
                public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                    if (response.code() == 200) {
                        tts.speak("Notification has been removed.", QUEUE_FLUSH, null, null);

                        alarmList.remove(position);     // 스와이프한 객체 삭제
                        notifyItemRemoved(position);
                        currentClickedPos = null;

                        return;
                    }
                    else if (response.code() == 401) {
                        tts.speak("Access by unauthorized members.", QUEUE_FLUSH, null, null);
                    }
                    else if (response.code() == 400) {
                        if (response.errorBody() != null) {
                            try {
                                String errorStr = response.errorBody().string();
                                JSONObject errorBody = new JSONObject(errorStr);
                                long errorIdx = errorBody.getLong("errorIdx");

                                if (errorIdx == 40001)  // 삭제 오류
                                    tts.speak("It cannot be deleted because it is a drug that has not been added to notifications.", QUEUE_FLUSH, null, null);
                            } catch (JSONException | IOException e) {
                                e.printStackTrace();
                            }
                        }
                        else
                            tts.speak("There was a problem deleting notifications.", QUEUE_FLUSH, null, null);
                    }
                    else {
                        tts.speak("There was a problem deleting notifications.", QUEUE_FLUSH, null, null);
                    }
                    notifyItemChanged(position);
                }

                @Override
                public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                    tts.speak("Can't connect to server.", QUEUE_FLUSH, null, null);
                    tts.playSilentUtterance(3000, TextToSpeech.QUEUE_ADD, null);
                    notifyItemChanged(position);
                }
            });
            alertDialog.dismiss();

            return true;
        });
        ibtDeleteAlarm.setOnClickListener(view -> {
            tts.speak("Undo Delete Notifications", QUEUE_FLUSH, null, null);

            notifyItemChanged(position);
            alertDialog.dismiss();
        });
    }

    public class AlarmViewHolder extends RecyclerView.ViewHolder {
        TextView tvAlarmName, tvAlarmDetails;

        AlarmViewHolder(final View itemView) {
            super(itemView);

            tvAlarmName = itemView.findViewById(R.id.tv_item_alarm_name);
            tvAlarmDetails = itemView.findViewById(R.id.tv_item_alarm_details);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (System.currentTimeMillis() > delay) {
                    currentClickedPos = pos;
                    delay = System.currentTimeMillis() + 3000;
                    tts.speak("Button. View " + alarmList.get(pos).getName() + " Details", QUEUE_FLUSH, null, null);
                } else if (currentClickedPos == pos) {
                    if (pos != RecyclerView.NO_POSITION) {
                        Intent medicineIntent = new Intent(context, MedicineResultActivity.class);
                        medicineIntent.putExtra("medicineIdx", alarmList.get(pos).getMedicineIdx());
                        context.startActivity(medicineIntent);
                    }
                }
            });

            tvAlarmDetails.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                currentClickedPos = pos;
                tts.speak("Text." + alarmList.get(pos).getName() + ((TextView) v).getText(), QUEUE_FLUSH, null, null);
            });
        }
    }
}
