package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;

import static com.nadoyagsa.pillaroid.MainActivity.tts;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nadoyagsa.pillaroid.adapter.FavoritesRecyclerAdapter;
import com.nadoyagsa.pillaroid.adapter.ItemTouchHelperCallback;
import com.nadoyagsa.pillaroid.data.FavoritesInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MypageFavoritesActivity extends AppCompatActivity {
    private boolean isSearching = false;
    private boolean isRecording = false;        // 볼륨 버튼을 위함
    private boolean isResultEnd = false;        // 음성 결과 반환 여부 확인 (음성 결과를 반환하기 전에 인식을 종료함 방지)
    private String searchKeyword = "";

    private ArrayList<FavoritesInfo> favoritesList;

    private FavoritesRecyclerAdapter favoritesAdapter;
    private Intent intent;
    private RecognitionListener recognitionListener;
    private SpeechRecognizer speechRecognizer;
    private TextView tvFavoritesInfo, tvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mypage_favorites);

        favoritesList = new ArrayList<>();

        Toolbar toolbar = findViewById(R.id.tb_favorites_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
            Objects.requireNonNull(actionBar).setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayHomeAsUpEnabled(false);
        View customView = View.inflate(this, R.layout.actionbar_mypage_favorites, null);
        ActionBar.LayoutParams params = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        actionBar.setCustomView(customView, params);
        toolbarListener(toolbar);
        tvTitle = toolbar.findViewById(R.id.tv_ab_favorites_title);

        RecyclerView rvFavorites = findViewById(R.id.rv_favorites_list);
        LinearLayoutManager favoritesManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL,false);
        rvFavorites.setLayoutManager(favoritesManager);
        favoritesAdapter = new FavoritesRecyclerAdapter(favoritesList);
        rvFavorites.setAdapter(favoritesAdapter);
        DividerItemDecoration devider = new DividerItemDecoration(this, 1);
        devider.setDrawable(Objects.requireNonNull(ResourcesCompat.getDrawable(getResources(), R.drawable.item_divide_bar, null)));
        rvFavorites.addItemDecoration(devider);

        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelperCallback(favoritesAdapter));
        helper.attachToRecyclerView(rvFavorites);

        tvFavoritesInfo = findViewById(R.id.tv_favorites_info);

        getFavoritesList();
        settingForSTT();
    }

    private void toolbarListener(Toolbar toolbar) {
        ImageView ivSearch = toolbar.findViewById(R.id.iv_ab_favorites_search);
        ivSearch.setOnClickListener(view -> {   // 즐겨찾기 목록 검색
            checkRecordPermission();

            isSearching = true;
        });
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
                            tts.speak("There are no medications set as favorites", QUEUE_FLUSH, null, null);
                            tvFavoritesInfo.setText(getString(R.string.text_favorites_no_result));
                        }
                        else {
                            tvFavoritesInfo.setVisibility(View.GONE);

                            for (int i=0; i<results.length(); i++) {
                                JSONObject favorites = results.getJSONObject(i);
                                favoritesList.add(new FavoritesInfo(favorites.getLong("favoritesIdx"), favorites.getInt("medicineIdx"), favorites.getString("medicineName")));
                            }
                            favoritesAdapter.setFavoritesWholeList();
                            favoritesAdapter.notifyDataSetChanged();

                            tts.speak("There are a total of " + favoritesList.size() + " favorites on the list.", QUEUE_FLUSH, null, null);
                        }
                    } catch (JSONException e) { e.printStackTrace(); }
                }
                else if (response.code() == 401) {
                    tts.speak("Access by unauthorized members. Return to the previous screen.", QUEUE_FLUSH, null, null);
                    tts.playSilentUtterance(7000, TextToSpeech.QUEUE_ADD, null);
                    finish();
                }
                else if (response.code() == 404) {
                    if (response.errorBody() != null) {
                        try {
                            String errorStr = response.errorBody().string();
                            JSONObject errorBody = new JSONObject(errorStr);
                            long errorIdx = errorBody.getLong("errorIdx");

                            if (errorIdx == 40403) {
                                tts.speak("There are no medications set as favorites", QUEUE_FLUSH, null, null);
                                tvFavoritesInfo.setText(getString(R.string.text_favorites_no_result));
                            }
                        } catch (JSONException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        tts.speak("There was a problem with the Favorites list lookup. Return to the previous screen.", QUEUE_FLUSH, null, null);
                        tts.playSilentUtterance(7000, TextToSpeech.QUEUE_ADD, null);
                        finish();
                    }
                }
                else {
                    tts.speak("There was a problem with the Favorites list lookup. Return to the previous screen.", QUEUE_FLUSH, null, null);
                    tts.playSilentUtterance(7000, TextToSpeech.QUEUE_ADD, null);
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                tts.speak("Can't connect to server. Return to the previous screen.", QUEUE_FLUSH, null, null);
                tts.playSilentUtterance(5000, TextToSpeech.QUEUE_ADD, null);
                finish();
            }
        });
    }

    private void speakRecordMethod() {
        tts.speak("Searching for favorites is possible with the volume buttons.", TextToSpeech.QUEUE_FLUSH, null, null);
        tts.playSilentUtterance(500, TextToSpeech.QUEUE_ADD, null);
        tts.speak("You can start and end recording with the volume button on the top.", TextToSpeech.QUEUE_ADD, null, null);
        tts.speak("You can stop recording with the lower volume button.", TextToSpeech.QUEUE_ADD, null, null);
    }

    private void checkRecordPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            // 녹음 권한이 없으면 권한 요청
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)== PackageManager.PERMISSION_DENIED) {
                tts.speak("Audio recording permission is required for voice recognition.", TextToSpeech.QUEUE_FLUSH, null, null);
                tts.speak("Click the Allow button on the far right of the center of the screen.", TextToSpeech.QUEUE_ADD, null, null);
                tts.speak("If permission is denied, return to the main screen.", TextToSpeech.QUEUE_ADD, null, null);

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1000);
            }
            else
                speakRecordMethod();
        }
        else {
            tts.speak("Speech recognition is not possible due to the low SDK version.", TextToSpeech.QUEUE_ADD, null, null);
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode==1000 && grantResults.length>0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                speakRecordMethod();
            else
                finish();
        }
    }

    private void settingForSTT() {
        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR");

        recognitionListener = new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {}
            @Override
            public void onBeginningOfSpeech() {
                isResultEnd = false;    // 결과 반환이 아직 되지 않았음
            }
            @Override
            public void onRmsChanged(float v) {}
            @Override
            public void onBufferReceived(byte[] bytes) {}
            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {    // 음성 인식 오류 발생 시
                String message;

                switch (error) {
                    case SpeechRecognizer.ERROR_AUDIO:
                        message = "Audio error";
                        break;
                    case SpeechRecognizer.ERROR_CLIENT:
                        return;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        message = "No permission";
                        break;
                    case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED:
                        message = "no language support";
                        break;
                    case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE:
                        message = "language not used";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK:
                        message = "network error";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        message = "network timeout";
                        break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        if (isRecording)
                            startRecord();
                        return;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        message = "recognition error";
                        break;
                    case SpeechRecognizer.ERROR_SERVER:
                        message = "server error";
                        break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        message = "recording timeout";
                        break;
                    case SpeechRecognizer.ERROR_SERVER_DISCONNECTED:
                        message = "server connection error";
                        break;
                    case SpeechRecognizer.ERROR_TOO_MANY_REQUESTS:
                        message = "too many requests";
                        break;
                    default:
                        message = "unknown error";
                        break;
                }
                speechRecognizer.cancel();
                tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
            }

            @Override
            public void onResults(Bundle bundle) {
                isResultEnd = true;

                ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                // 인식 결과
                StringBuilder newText = new StringBuilder();
                for (int i=0; i<matches.size() ; i++) {
                    newText.append(matches.get(i));
                }

                searchKeyword = searchKeyword.concat(newText.toString().replaceAll("\\s", ""));

                if (isRecording)    // 인식 종료 버튼이 아직 눌리지 않음 (녹음 재개)
                    speechRecognizer.startListening(intent);
                else {              // 인식 종료 버튼이 눌렸을 때, 종료 시점 이후에 결과가 반환이 되는 경우
                    tvTitle.setText(searchKeyword);
                    favoritesAdapter.searchFavoritesList(searchKeyword);
                    tts.speak("The list of favorites searched for " + searchKeyword + " is a total of " + favoritesAdapter.getItemCount() + ".", TextToSpeech.QUEUE_FLUSH, null, null);

                    searchKeyword = "";
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {}
            @Override
            public void onEvent(int i, Bundle bundle) {}
        };
    }

    // 음성 인식 시작
    private void startRecord() {
        isRecording = true;

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
        speechRecognizer.setRecognitionListener(recognitionListener);
        speechRecognizer.startListening(intent);
    }
    // 음성 인식 종료
    private void endRecord() {
        isRecording = false;
        speechRecognizer.stopListening();

        // 인식 결과가 모두 출력되었을 경우
        if (isResultEnd) {

            tvTitle.setText(searchKeyword);
            favoritesAdapter.searchFavoritesList(searchKeyword);
            tts.speak("The list of favorites searched for " + searchKeyword + " is a total of " + favoritesAdapter.getItemCount() + ".", TextToSpeech.QUEUE_FLUSH, null, null);

            searchKeyword = "";
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP: {      // 음성 인식 시작 및 종료
                if (isSearching) {
                    if (isRecording)
                        endRecord();
                    else {
                        tts.stop();
                        startRecord();
                    }
                }
                return true;
            }
            case KeyEvent.KEYCODE_VOLUME_DOWN: {    // 음성 인식 취소
                if (isSearching) {
                    if (isRecording) {
                        speechRecognizer.cancel();

                        isRecording = false;
                        searchKeyword = "";
                    }
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (isSearching) {      // 즐겨찾기 검색 목록 조회하는 경우에는 회원의 즐겨찾기 목록 전체가 보여지게 됨
            favoritesAdapter.searchFavoritesList("");
            favoritesAdapter.notifyDataSetChanged();

            tvTitle.setText(getString(R.string.text_favorites_list));
            tts.speak("Return to the full list of favorites.", TextToSpeech.QUEUE_FLUSH, null, null);

            isSearching = false;
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }
}
