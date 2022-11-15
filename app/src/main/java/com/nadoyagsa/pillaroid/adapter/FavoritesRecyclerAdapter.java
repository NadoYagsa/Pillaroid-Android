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
import com.nadoyagsa.pillaroid.data.FavoritesInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoritesRecyclerAdapter extends RecyclerView.Adapter<FavoritesRecyclerAdapter.FavoritesViewHolder> implements ItemTouchHelperListener {
    private Context context;
    private ArrayList<FavoritesInfo> favoritesList;
    private final ArrayList<FavoritesInfo> favoritesWholeList;    // 검색 시 필요한 전체 즐겨찾기 목록

    private long delay = 0;
    private Integer currentClickedPos = null;

    public FavoritesRecyclerAdapter(ArrayList<FavoritesInfo> favoritesList) {
        this.favoritesList = favoritesList;

        favoritesWholeList = new ArrayList<>();
    }

    public void setFavoritesWholeList() {
        favoritesWholeList.clear();
        favoritesWholeList.addAll(favoritesList);
    }

    public void searchFavoritesList(String keyword) {
        favoritesList = (ArrayList<FavoritesInfo>) favoritesWholeList.stream().filter(favoritesInfo -> favoritesInfo.getMedicineName().contains(keyword)).collect(Collectors.toList());
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FavoritesRecyclerAdapter.FavoritesViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.item_favorites, parent, false);
        FavoritesRecyclerAdapter.FavoritesViewHolder viewHolder = new FavoritesRecyclerAdapter.FavoritesViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull FavoritesRecyclerAdapter.FavoritesViewHolder holder, int position) {
        holder.tvMedicineName.setText(favoritesList.get(position).getMedicineName());
    }

    @Override
    public int getItemCount() { return favoritesList.size(); }

    @Override
    public void onItemSwipe(int position) {     // 즐겨찾기 삭제
        tts.speak("Are you sure you want to delete " + favoritesList.get(position).getMedicineName() + "'s favorites?", TextToSpeech.QUEUE_FLUSH, null, null);

        View deleteFavoritesDialogView = View.inflate(context, R.layout.dialog_delete, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(deleteFavoritesDialogView);
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

        AppCompatImageButton ibtDeleteFavorites = deleteFavoritesDialogView.findViewById(R.id.ibt_dialog_delete);
        // 휴지통 이미지 버튼을 꾹 누르면 삭제, 클릭하면 취소
        ibtDeleteFavorites.setOnLongClickListener(view -> {
            PillaroidAPIImplementation.getApiService().deleteFavorites(SharedPrefManager.read("token", null), favoritesList.get(position).getFavoritesIdx()).enqueue(new Callback<String>() {
                @Override
                public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                    if (response.code() == 200) {
                        tts.speak("Delete Favorites", TextToSpeech.QUEUE_FLUSH, null, null);

                        favoritesWholeList.remove(favoritesList.get(position));
                        favoritesList.remove(position);     // 스와이프한 객체 삭제
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
                                    tts.speak("Since it is a drug that has not been added to favorites, it cannot be deleted.", QUEUE_FLUSH, null, null);
                            } catch (JSONException | IOException e) {
                                e.printStackTrace();
                            }
                        }
                        else
                            tts.speak("There was a problem deleting favorites.", QUEUE_FLUSH, null, null);
                    }
                    else {
                        tts.speak("There was a problem deleting favorites.", QUEUE_FLUSH, null, null);
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
        ibtDeleteFavorites.setOnClickListener(view -> {
            tts.speak("Undo Delete Favorites", QUEUE_FLUSH, null, null);

            notifyItemChanged(position);
            alertDialog.dismiss();
        });
    }

    public class FavoritesViewHolder extends RecyclerView.ViewHolder {
        AppCompatImageButton ibtStar;
        TextView tvMedicineName;

        FavoritesViewHolder(final View itemView) {
            super(itemView);

            ibtStar = itemView.findViewById(R.id.ibt_item_favorites_star);
            tvMedicineName = itemView.findViewById(R.id.tv_item_favorites_name);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (System.currentTimeMillis() > delay) {
                    currentClickedPos = pos;
                    delay = System.currentTimeMillis() + 3000;
                    tts.speak("Button." + favoritesList.get(pos).getMedicineName(), QUEUE_FLUSH, null, null);
                } else if (currentClickedPos == pos) {
                    if (pos != RecyclerView.NO_POSITION) {
                        Intent medicineIntent = new Intent(context, MedicineResultActivity.class);
                        medicineIntent.putExtra("medicineIdx", favoritesList.get(pos).getMedicineIdx());
                        context.startActivity(medicineIntent);
                    }
                }
            });
        }
    }
}
