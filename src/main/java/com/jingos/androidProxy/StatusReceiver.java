package com.jingos.androidProxy;

import static com.jingos.androidProxy.ProxyService.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Bundle;
import android.util.Log;

/**
 * Author: yangjin
 * Time: 2021/8/21  下午12:44
 * Description: This is StatusReceiver
 */
public class StatusReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: start...............");
        Bundle extras = intent.getExtras();
        String packageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME);
        int status = extras.getInt(PackageInstaller.EXTRA_STATUS);
        String message = extras.getString(PackageInstaller.EXTRA_STATUS_MESSAGE);
        if (intent.getAction().equals(ProxyService.PACKAGE_INSTALLED_ACTION)) {
            Log.d(TAG, "onReceive: " + intent.getAction());
            switch (status) {
                case PackageInstaller.STATUS_SUCCESS:
                    Log.d(TAG, "onReceive: " + packageName + " install success!");
                    StatusCode.status = status;
                    break;
                case PackageInstaller.STATUS_FAILURE:
                case PackageInstaller.STATUS_FAILURE_ABORTED:
                case PackageInstaller.STATUS_FAILURE_BLOCKED:
                case PackageInstaller.STATUS_FAILURE_CONFLICT:
                case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                case PackageInstaller.STATUS_FAILURE_INVALID:
                case PackageInstaller.STATUS_FAILURE_STORAGE:
                    Log.e(TAG, "onReceive: " + packageName + " Install Failed ");
                    StatusCode.status = status;
                    System.err.println(message);
                    break;
            }
        } else if (intent.getAction().equals(ProxyService.PACKAGE_UNINSTALLED_ACTION)) {
            Log.d(TAG, "onReceive: " + intent.getAction());
            switch (status) {
                case PackageInstaller.STATUS_SUCCESS:
                    Log.d(TAG, "onReceive: " + packageName +  " uninstall success");
                    StatusCode.status = status;
                    break;
                case PackageInstaller.STATUS_FAILURE:
                case PackageInstaller.STATUS_FAILURE_ABORTED:
                case PackageInstaller.STATUS_FAILURE_BLOCKED:
                case PackageInstaller.STATUS_FAILURE_CONFLICT:
                case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                case PackageInstaller.STATUS_FAILURE_INVALID:
                case PackageInstaller.STATUS_FAILURE_STORAGE:
                    Log.e(TAG, "onReceive: " + packageName +  " uninstall failed, status : " + status);
                    StatusCode.status = status;
                    System.err.println(message);
                    break;
            }
        }
        StatusCode.lock.lock();
        StatusCode.status_changed.signalAll();
        StatusCode.lock.unlock();
    }
}
