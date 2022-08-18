package com.nadoyagsa.pillaroid;

import static android.speech.tts.TextToSpeech.ERROR;
import static android.speech.tts.TextToSpeech.SUCCESS;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchPillActivity extends AppCompatActivity {
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_pill);

        tts = new TextToSpeech(this, status -> {
            if (status == SUCCESS) {
                int result = tts.setLanguage(Locale.KOREAN);
                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language is not supported");
                }
                tts.setSpeechRate(SharedPrefManager.read("voiceSpeed", (float) 1));

                tts.speak("후면 카메라가 켜졌습니다. 손에 알약을 놓고 카메라 뒤로 위치시켜주세요.", TextToSpeech.QUEUE_FLUSH, null, null);
            } else if (status != ERROR) {
                Log.e("TTS", "Initialization Failed");
            }
        });

        // TODO: 알약 이미지 첨부 방식
        MultipartBody.Part pillImage = MultipartBody.Part.createFormData("pillImage", /*파일 이름.확장자*/, RequestBody.create(MediaType.parse("multipart/form-data"), /*파일 내용 혹은 파일 byte[]*/));
        PillaroidAPIImplementation.getApiService().postPillByImage(pillImage).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.code() == 200) {
                    // TODO: 성공 시 내용
                }
                else {
                    // TODO: 실패 시 내용
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                // TODO: 실패 시 내용
            }
        });

        /* TODO: 알약을 인식 후 검색 결과 확인 */
        //startActivity(new Intent(this, MedicineResultActivity.class));
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
