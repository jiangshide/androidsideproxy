/*
 * Copyright (C) 2021 JingOS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jingos.androidProxy;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class ProxyService extends Service {
    public static final String PACKAGE_INSTALLED_ACTION =
            "com.jingos.androidProxy.content.SESSION_API_PACKAGE_INSTALLED";
    public static final String PACKAGE_UNINSTALLED_ACTION =
            "com.jingos.androidProxy.content.SESSION_API_PACKAGE_UNINSTALLED";
    public static final String TAG = "android_side_proxy";
    private Thread T;
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "-------android side proxy start----------");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "-------service starting----------");
        createNotificationChannel();
        T = new Thread(new ProxyServiceRunnable(this));
        T.start();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // 通知渠道的id
        String id = "android_proxy_channel";
        // 用户可以看到的通知渠道的名字.
        CharSequence name = "android_proxy_channel";

        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel mChannel = new NotificationChannel(id, name, importance);

//         最后在notificationmanager中创建该通知渠道 //
        mNotificationManager.createNotificationChannel(mChannel);

        // 为该通知设置一个id
        int notifyID = 1;
        // Create a notification and set the notification channel.
        Notification notification = new Notification.Builder(this, id)
                .setContentTitle("New Message").setContentText("You've received new messages.")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
        startForeground(notifyID, notification);
    }
}
