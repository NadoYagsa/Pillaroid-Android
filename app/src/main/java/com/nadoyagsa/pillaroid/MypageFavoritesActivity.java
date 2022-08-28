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
                            tts.speak("즐겨찾기를 설정한 의약품이 없습니다.", QUEUE_FLUSH, null, null);
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

                            tts.speak("즐겨찾기 목록은 총 "+favoritesList.size()+"개 입니다.", QUEUE_FLUSH, null, null);
                        }
                    } catch (JSONException e) { e.printStackTrace(); }
                }
                else if (response.code() == 401) {
                    tts.speak("허가받지 않은 회원의 접근입니다. 이전 화면으로 돌아갑니다.", QUEUE_FLUSH, null, null);
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
                                tts.speak("즐겨찾기를 설정한 의약품이 없습니다.", QUEUE_FLUSH, null, null);
                                tvFavoritesInfo.setText(getString(R.string.text_favorites_no_result));
                            }
                        } catch (JSONException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        tts.speak("즐겨찾기 목록 조회에 문제가 생겼습니다. 이전 화면으로 돌아갑니다.", QUEUE_FLUSH, null, null);
                        tts.playSilentUtterance(7000, TextToSpeech.QUEUE_ADD, null);
                        finish();
                    }
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

    private void speakRecordMethod() {
        tts.speak("즐겨찾기 검색은 음량 버튼으로 가능합니다.", TextToSpeech.QUEUE_FLUSH, null, null);
        tts.playSilentUtterance(500, TextToSpeech.QUEUE_ADD, null);
        tts.speak("상단 음량 버튼으로 녹음을 시작 및 종료를 할 수 있습니다.", TextToSpeech.QUEUE_ADD, null, null);
        tts.speak("하단 음량 버튼으로 녹음을 중지할 수 있습니다.", TextToSpeech.QUEUE_ADD, null, null);
    }

    private void checkRecordPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            // 녹음 권한이 없으면 권한 요청
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)== PackageManager.PERMISSION_DENIED) {
                tts.speak("음성인식을 위해 오디오녹음 권한이 필요합니다.", TextToSpeech.QUEUE_FLUSH, null, null);
                tts.speak("화면 중앙의 가장 우측에 있는 허용 버튼을 눌러주세요.", TextToSpeech.QUEUE_ADD, null, null);
                tts.speak("권한 거부 시에는 메인 화면으로 돌아갑니다.", TextToSpeech.QUEUE_ADD, null, null);

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1000);
            }
            else
                speakRecordMethod();
        }
        else {
            tts.speak("SDK 버전이 낮아 음성 인식이 불가합니다.", TextToSpeech.QUEUE_ADD, null, null);
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
                        message = "오디오 에러";
                        break;
                    case SpeechRecognizer.ERROR_CLIENT:
                        return;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        message = "권한 없음";
                        break;
                    case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED:
                        message = "언어 지원 안함";
                        break;
                    case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE:
                        message = "언어 사용 안됨";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK:
                        message = "네트워크 오류";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        message = "네트워크 시간 초과";
                        break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        if (isRecording)
                            startRecord();
                        return;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        message = "인식 오류";
                        break;
                    case SpeechRecognizer.ERROR_SERVER:
                        message = "서버 오류";
                        break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        message = "녹음 시간 초과";
                        break;
                    case SpeechRecognizer.ERROR_SERVER_DISCONNECTED:
                        message = "서버 연결 오류";
                        break;
                    case SpeechRecognizer.ERROR_TOO_MANY_REQUESTS:
                        message = "요청 과다";
                        break;
                    default:
                        message = "알 수 없는 오류";
                        break;
                }
                speechRecognizer.cancel();
                tts.speak(message.concat(" 문제가 발생하였습니다."), TextToSpeech.QUEUE_FLUSH, null, null);
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
                    tts.speak(searchKeyword.concat("로 검색된 즐겨찾기 목록은 총 "+favoritesAdapter.getItemCount()+"개 입니다."), TextToSpeech.QUEUE_FLUSH, null, null);

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
            tts.speak(searchKeyword.concat("로 검색된 즐겨찾기 목록은 총 "+favoritesAdapter.getItemCount()+"개 입니다."), TextToSpeech.QUEUE_FLUSH, null, null);

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
            tts.speak("전체 즐겨찾기 목록으로 돌아갑니다.", TextToSpeech.QUEUE_FLUSH, null, null);

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
