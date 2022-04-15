package com.nadoyagsa.pillaroid;

import android.app.Application;

import com.kakao.sdk.common.KakaoSdk;

public class PillaroidApplication extends Application {
    private static PillaroidApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        KakaoSdk.init(this, getResources().getString(R.string.KAKAO_NATIVE_APP_KEY));
    }
}
