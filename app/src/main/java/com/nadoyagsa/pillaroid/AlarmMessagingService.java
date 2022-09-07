package com.nadoyagsa.pillaroid;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class AlarmMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(@NonNull String token) {
        if (!SharedPrefManager.read("alarm_token", "").equals(""))
            SharedPrefManager.remove("alarm_token");
        SharedPrefManager.write("alarm_token", token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            String title = remoteMessage.getData().get("title");
            String body = remoteMessage.getData().get("body");

            Intent intent = new Intent(this, MypageAlarmActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);    // 상위 스택 제거
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

            final String CHANNEL_ID = "PILLAROID_";
            NotificationManager mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                final String CHANNEL_NAME = "PillaroidChannel";
                final String CHANNEL_DESCRIPTION = "필라로이드 알림";
                final int importance = NotificationManager.IMPORTANCE_HIGH;

                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
                mChannel.setDescription(CHANNEL_DESCRIPTION);
                mChannel.enableLights(true);
                mChannel.enableVibration(true);
                mChannel.setVibrationPattern(new long[]{100, 200, 100, 200});
                mChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                mManager.createNotificationChannel(mChannel);
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setAutoCancel(true)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setContentTitle(title)
                    .setContentText(body)
                    .setContentIntent(contentIntent)
                    .setOnlyAlertOnce(true);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                builder.setContentTitle(title);
                builder.setVibrate(new long[]{500, 500});
            }
            mManager.notify(0, builder.build());
        }
    }
}
