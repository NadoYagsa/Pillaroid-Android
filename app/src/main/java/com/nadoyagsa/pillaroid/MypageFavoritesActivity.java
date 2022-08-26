package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;
import static android.speech.tts.TextToSpeech.SUCCESS;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nadoyagsa.pillaroid.adapter.FavoritesRecyclerAdapter;
import com.nadoyagsa.pillaroid.data.FavoritesInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MypageFavoritesActivity extends AppCompatActivity {
    private ArrayList<FavoritesInfo> favoritesList;

    private FavoritesRecyclerAdapter favoritesAdapter;
    private TextToSpeech tts;
    private TextView tvFavoritesInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mypage_favorites);

        favoritesList = new ArrayList<>();

        tts = new TextToSpeech(this, status -> {
            if (status == SUCCESS) {
                int result = tts.setLanguage(Locale.KOREAN);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language is not supported");
                }
                tts.setSpeechRate(SharedPrefManager.read("voiceSpeed", (float) 1));

                getFavoritesList();
            } else if (status != ERROR) {
                Log.e("TTS", "Initialization Failed");
            }
        });

        Toolbar toolbar = findViewById(R.id.tb_favorites_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
            Objects.requireNonNull(actionBar).setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
        View customView = View.inflate(this, R.layout.actionbar_mypage_favorites, null);
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        actionBar.setCustomView(customView, params);

        RecyclerView rvFavorites = findViewById(R.id.rv_favorites_list);
        LinearLayoutManager favoritesManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL,false);
        rvFavorites.setLayoutManager(favoritesManager);
        favoritesAdapter = new FavoritesRecyclerAdapter(favoritesList);
        rvFavorites.setAdapter(favoritesAdapter);
        DividerItemDecoration devider = new DividerItemDecoration(this, 1);
        devider.setDrawable(Objects.requireNonNull(ResourcesCompat.getDrawable(getResources(), R.drawable.item_divide_bar, null)));
        rvFavorites.addItemDecoration(devider);

        tvFavoritesInfo = findViewById(R.id.tv_favorites_info);
    }

    private void getFavoritesList() {
        tvFavoritesInfo.setVisibility(View.VISIBLE);
        tvFavoritesInfo.setText(getString(R.string.text_favorites_search));

        favoritesList.clear();
        favoritesAdapter.notifyDataSetChanged();
        PillaroidAPIImplementation.getApiService().getFavoritesList(SharedPrefManager.read("token", "")).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.code() == 200) {
                    try {
                        JSONObject responseJson = new JSONObject(Objects.requireNonNull(response.body()));
                        JSONArray results = responseJson.getJSONArray("data");

                        if (results.length() == 0) {
                            tts.speak("즐겨찾기를 설정한 의약품이 없습니다.", QUEUE_FLUSH, null, null);
                            tvFavoritesInfo.setText(getString(R.string.text_favorites_no_result));
                        }
                        else {
                            tvFavoritesInfo.setVisibility(View.GONE);

                            for (int i=0; i<results.length(); i++) {
                                JSONObject favorites = results.getJSONObject(i);
                                favoritesList.add(new FavoritesInfo(favorites.getLong("favoritesIdx"), favorites.getInt("medicineIdx"), favorites.getString("medicineName")));
                            }
                            favoritesAdapter.notifyDataSetChanged();

                            tts.speak("즐겨찾기 목록은 총 "+favoritesList.size()+"개 입니다.", TextToSpeech.QUEUE_FLUSH, null, null);
                        }
                    } catch (JSONException e) { e.printStackTrace(); }
                }
                else if (response.code() == 401) {
                    tts.speak("허가받지 않은 회원의 접근입니다. 이전 화면으로 돌아갑니다.", QUEUE_FLUSH, null, null);
                    tts.playSilentUtterance(7000, TextToSpeech.QUEUE_ADD, null);
                    finish();
                }
                else if (response.code() == 40403) {
                    tts.speak("즐겨찾기를 설정한 의약품이 없습니다.", QUEUE_FLUSH, null, null);
                    tvFavoritesInfo.setText(getString(R.string.text_favorites_no_result));
                }
                else {
                    tts.speak("즐겨찾기 목록 조회에 문제가 생겼습니다. 이전 화면으로 돌아갑니다.", QUEUE_FLUSH, null, null);
                    tts.playSilentUtterance(7000, TextToSpeech.QUEUE_ADD, null);
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                tts.speak("서버와 연결이 되지 않습니다. 이전 화면으로 돌아갑니다.", QUEUE_FLUSH, null, null);
                tts.playSilentUtterance(5000, TextToSpeech.QUEUE_ADD, null);
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            //tts 자원 해제
            if (tts != null) {
                tts.stop();
                tts.shutdown();
                tts = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
