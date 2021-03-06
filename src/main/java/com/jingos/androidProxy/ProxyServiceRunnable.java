package com.jingos.androidProxy;

import static android.app.PendingIntent.FLAG_ONE_SHOT;
import static com.jingos.androidProxy.ProxyService.TAG;

import com.jingos.androidProxy.JNIMessage.AppInfoMessage;
import com.jingos.androidProxy.JNIMessage.InstallAppMessage;
import com.jingos.androidProxy.JNIMessage.WidgetInfoMessage;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.graphics.Rect;
import android.app.ActivityTaskManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;
import androidx.annotation.RequiresApi;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.os.IBinder;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import com.jingos.IBinderWLManagerService;

public class ProxyServiceRunnable implements Runnable {
    private final Context context;
    private final PackageManager mPm;


    public ProxyServiceRunnable(ProxyService service) {
        context = service.getApplicationContext();
        mPm = context.getPackageManager();
    }
    // load C/C++ library
    static {
        System.loadLibrary("native_jing_android_proxy");
    }

    @Override
    public void run() {
        Log.d(TAG, "start connect...");
        nativeStartServer("/wayland/service_proxy");
    }

    /**
     * @Date:  2021/8/2
     * @Description: native??????, ?????????JingOs ??????????????????
     * @param  socket_path socket????????????
     * @return void
     */
    private native void nativeStartServer(String socket_path);

    /**
    * @Date: 2021/8/18
    * @Description: native ????????????????????????
    * @param
    * @return: void
    */
    private native void nativeStopServer();

    /**
     * @Date: 2021/8/2
     * @Description: ????????????
     * @param  packageName ????????????/????????????+??????
     * @return:
     */
    public int startApp(String packageName) {
        Intent intent;
        boolean isContainClassName = packageName.contains("/");
        if (!isContainClassName) {
            intent =  mPm.getLaunchIntentForPackage(packageName);
        } else {
            Log.d(TAG, "???className");
            String name = packageName.substring(0, packageName.indexOf("/"));
            String className = packageName.substring(packageName.indexOf("/") + 1);
            ComponentName componentName = new ComponentName(name, className);
            intent = new Intent();
            intent.setComponent(componentName);
        }
        if (null == intent) {
            Log.e(TAG, "startApp:????????????????????????????????????");
            return StatusCode.appStartFailed;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT);
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchWindowingMode(5);
        context.startActivity(intent, options.toBundle());
        return StatusCode.appStartSuccess;
    }

    /**
    * @Date: 2021/9/11
    * @Description: ???????????????????????????scheme????????????????????????
    * @param
    * @return: {@link StatusCode} ?????????
    */
    public int startAppFromWidget(String jsonStr) {
        // ??????json??????????????????scheme
        Log.d(TAG, "startAppFromWidget: " + jsonStr);
        JSONObject jsonObject;
        String scheme;
        try {
            jsonObject = new JSONObject(jsonStr);
            scheme =  jsonObject.get("scheme").toString();
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "startAppFromWidget: " + e.getMessage());
            return StatusCode.appStartFailed;
        }
        Log.d(TAG, "startAppFromWidget: " + scheme);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(scheme));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        List<ResolveInfo> activities = mPm.queryIntentActivities(intent, 0);
        boolean isValid = !activities.isEmpty();
        if (isValid) {
            context.startActivity(intent);
            return StatusCode.appStartSuccess;
        }
        return StatusCode.appStartFailed;
    }

    /**
    * @Date: 2021/9/10
    * @Description: ????????????????????????????????????
    * @param
    * @return: {@link WidgetInfoMessage} ?????????????????????????????????????????????json?????????
    */
    public WidgetInfoMessage getAllWidgetInfo() {
        WidgetInfoMessage widgetInfoMessage = new WidgetInfoMessage();
        Uri uri;
        int i = 0;
        try {
            uri = Uri.parse("content://com.jn.note.provider.NoteProvider");
        } catch (NullPointerException e) {
            widgetInfoMessage.statusCode = StatusCode.getAllWidgetInfoFailed;
            e.printStackTrace();
            return widgetInfoMessage;
        }
        ContentResolver contentResolver = context.getContentResolver();
        String recentOpen;
        // ?????????????????????contentProvider?????????
        do {
           recentOpen = contentResolver.getType(uri);
           Log.d(TAG, "getAllWidgetInfo: " + i);
           i++;
           try {
               TimeUnit.MICROSECONDS.sleep(10 * 1000);
           } catch (InterruptedException e) {
               e.printStackTrace();
           }
        } while ((recentOpen == null) && (i < 5));
        if (recentOpen == null) {
            widgetInfoMessage.statusCode = StatusCode.getAllWidgetInfoFailed;
            return widgetInfoMessage;
        }
        Log.d(TAG, "getAllWidgetInfo: " + recentOpen);
        // ??????json,???????????????base64????????????????????????????????????json
        HandleJson handleJson = new HandleJson(recentOpen);
        String newJsonStr = null;
        try {
            JSONObject jsonObject = new JSONObject(recentOpen);
            ObtainAppInfo obtainAppInfo = new ObtainAppInfo(mPm, jsonObject.getString("pkg_name"));
            newJsonStr =  handleJson.assembleJson(obtainAppInfo.getAppName());
        } catch (JSONException | PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "getAllWidgetInfo: " + e.getMessage());
            widgetInfoMessage.statusCode = StatusCode.getAllWidgetInfoFailed;
            return widgetInfoMessage;
        }
        widgetInfoMessage.statusCode = StatusCode.getAllWidgetInfoSuccess;
        widgetInfoMessage.recentOpen = newJsonStr;
        return widgetInfoMessage;
    }

     /**
     * @Date: 2021/11/23
     * @Description: ??????packageName???????????????task
     * @return: {@link ActivityManager.RunningTaskInfo} task
     */
    private ActivityManager.RunningTaskInfo getSpecificTask (List<ActivityManager.RunningTaskInfo> tasks ,String packageName) {
        try {
            tasks = ActivityTaskManager.getService().getTasks(100);
        } catch (SecurityException | RemoteException e) {
            Log.e(TAG, "getTaskId: Failed to get runningTasks : ", e);
            e.printStackTrace();
            return null;
        }
        for (ActivityManager.RunningTaskInfo task : tasks) {
            String baseActivityPackageName = task.baseActivity.getPackageName();
            Log.d(TAG, "getSpecificTask: name : " +  baseActivityPackageName);
            if (baseActivityPackageName.equals(packageName)) {
                return task;
            }
        }
        return null;
    }

    /**
     * @Date: 2021/8/2
     * @Description: ?????????????????????????????????Activity????????????????????????????????????
     * @param packageName ????????????
     * @return: {@link StatusCode} ?????????
     */
    public int stopApp(String packageName) {
        List<ActivityManager.RunningTaskInfo> tasks = new ArrayList<>();
        boolean isStopSuccess = false;
        Log.d(TAG, "??????stop");
        ActivityManager.RunningTaskInfo task = getSpecificTask(tasks, packageName);
        if (task == null) {
            return StatusCode.appStopFailed;
        }
        try {
            isStopSuccess = ActivityTaskManager.getService().jingOsStopActivity(task.taskId);
        } catch (RemoteException e) {
            Log.e(TAG, "stopApp:", e);
        }
        Log.d(TAG, "isStopSuccess : " + isStopSuccess);
        return isStopSuccess ? StatusCode.appStopSuccess : StatusCode.appStopFailed;
    }

    /**
    * @Date: 2021/8/2
    * @Description: ?????????????????????????????????
    * @param packageName ????????????
    * @return: {@link StatusCode} ?????????
    */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public int killApp(String packageName){
        List<ActivityManager.RunningTaskInfo> tasks = new ArrayList<>();
        boolean isRemove = false;
        ActivityManager.RunningTaskInfo task = getSpecificTask(tasks, packageName);
        if (task == null) {
            return StatusCode.appKillFailed;
        }
        try {
            isRemove = ActivityTaskManager.getService().removeTask(task.taskId);
            tasks.remove(task);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to remove task=" + task.taskId, e);
            e.printStackTrace();
        }

        IBinder binder = null;
        Class<?> serviceManager = null;
        try {
            serviceManager = Class.forName("android.os.ServiceManager");
            Method getServiceMethod = serviceManager.getDeclaredMethod("getService", String.class);
            binder = (IBinder)getServiceMethod.invoke(null, "wlmanagerservice");
            if (binder == null) {
                Log.d(TAG, "getService failed!");
                return StatusCode.appKillFailed;
            }
            Log.d(TAG, "found service!");
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        IBinderWLManagerService service = IBinderWLManagerService.Stub.asInterface(binder);
        try {
            Log.d(TAG, "??????wl_manager????????????");
            service.DestroyApp(packageName);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

       if (isRemove) {
           return StatusCode.appKillSuccess;
       } else {
           Log.d(TAG, "killApp: " + packageName + " ??????");
       }
        return StatusCode.appKillFailed;
    }

    /**
     * @Date: 2021/8/2
     * @Description: ????????????
     * @param apkPath ????????????????????????
     * @param option ????????????????????????null
     * @return: {@link InstallAppMessage} ????????? + appInfo
     */
    public InstallAppMessage installApp(String apkPath, String option) {
        InstallAppMessage installAppMessage = new InstallAppMessage();
        PackageInstaller.Session session = null;
        long sizeBytes;
        String absolutePath;
        Log.d(TAG, "installApp : " + apkPath);
        absolutePath = "/data/" + apkPath;
        File file = new File(absolutePath);
        if (!file.exists() && file.isDirectory()) {
            installAppMessage.statusCode = StatusCode.appInstallFailed;
            Log.e(TAG, "installApp: ???????????????");
            return installAppMessage;
        }
        sizeBytes = file.length();
        Log.d(TAG, "File size : " + sizeBytes);
        String packageName = new ObtainAppInfo.PackageNameGet(mPm, absolutePath).getPackageName();
        try {
            PackageInstaller packageInstaller = mPm.getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            int sessionId = packageInstaller.createSession(params);
            session = packageInstaller.openSession(sessionId);
            OutputStream packageInSession = session.openWrite(packageName, 0, sizeBytes);
            InputStream is = new FileInputStream(absolutePath);
            byte[] buffer = new byte[16384];
            int n;
            while ((n = is.read(buffer)) >= 0) {
                packageInSession.write(buffer, 0, n);
            }
            // ?????? ??????????????????????????????????????????
            session.fsync(packageInSession);
            packageInSession.close();
            is.close();
            Log.d(TAG, "Success streamed apk !");

            // Create an install status receiver.
            Intent intent = new Intent(context, StatusReceiver.class);
            intent.setAction(ProxyService.PACKAGE_INSTALLED_ACTION);
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, FLAG_ONE_SHOT);
            IntentSender statusReceiver = pendingIntent.getIntentSender();

            // Commit the session (this will start the installation workflow).
            session.commit(statusReceiver);

            // ?????????????????? install ?????????status ??????
            try {
                StatusCode.lock.lock();
                StatusCode.status_changed.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                StatusCode.lock.unlock();
            }
            switch (StatusCode.status) {
                case PackageInstaller.STATUS_SUCCESS:
                    ObtainAppInfo obtainAppInfo = new ObtainAppInfo(mPm,packageName);
                    installAppMessage.statusCode = StatusCode.appInstallSuccess;
                    installAppMessage.appName = new String[1][2];
                    installAppMessage.appName[0][0] = "";
                    installAppMessage.appName[0][1] = obtainAppInfo.getAppName();
                    installAppMessage.packageName = packageName;
                    installAppMessage.appVersion = obtainAppInfo.getVersionName();
                    installAppMessage.iconRelativePath = obtainAppInfo.getAppIcon();
                    return installAppMessage;
                case PackageInstaller.STATUS_FAILURE:
                case PackageInstaller.STATUS_FAILURE_ABORTED:
                case PackageInstaller.STATUS_FAILURE_BLOCKED:
                case PackageInstaller.STATUS_FAILURE_CONFLICT:
                case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                case PackageInstaller.STATUS_FAILURE_INVALID:
                    break;
                case PackageInstaller.STATUS_FAILURE_STORAGE:
                    installAppMessage.statusCode = StatusCode.appInstallInsufficientDiskSpace;
                    return installAppMessage;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            if (session != null) {
                session.abandon();
            }
            e.printStackTrace();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "installApp: " + packageName + "NotFound !");
        }
        installAppMessage.statusCode = StatusCode.appInstallFailed;
        return installAppMessage;
    }

    /**
     * @Date: 2021/8/12
     * @Description: ????????????????????????
     * @param packageName ????????????
     * @param option ???????????????????????? null???
     * @return: {@link StatusCode}
     */
    public int uninstallApp(String packageName, String option) {
        try {
            PackageInfo packageInfo =  mPm.getPackageInfo(packageName, 0);
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM) {
                Log.e(TAG, "uninstallApp: ??????????????????????????????");
                return StatusCode.appUninstallFailed;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "uninstallApp: " + e.getMessage());
            e.printStackTrace();
            return StatusCode.appUninstallFailed;
        }
        PackageInstaller installer = mPm.getPackageInstaller();
        Intent intent = new Intent(context, StatusReceiver.class);
        intent.setAction(ProxyService.PACKAGE_UNINSTALLED_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,0,intent,0);
        installer.uninstall(packageName, pendingIntent.getIntentSender());

        // ?????????????????? unInstall ?????????status ??????
        try {
            StatusCode.lock.lock();
            StatusCode.status_changed.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            StatusCode.lock.unlock();
        }
        switch (StatusCode.status) {
            case PackageInstaller.STATUS_SUCCESS:
                return StatusCode.appUninstallSuccess;
            case PackageInstaller.STATUS_FAILURE:
            case PackageInstaller.STATUS_FAILURE_ABORTED:
            case PackageInstaller.STATUS_FAILURE_BLOCKED:
            case PackageInstaller.STATUS_FAILURE_CONFLICT:
            case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
            case PackageInstaller.STATUS_FAILURE_INVALID:
            case PackageInstaller.STATUS_FAILURE_STORAGE:
                // TODO ??????????????????
                break;
        }
        return StatusCode.appUninstallFailed;
    }

    /**
     * @Date: 2021/8/2
     * @Description: ??????????????????????????????
     * @param isAll ???????????????false??????PackageInfo?????????PackageName????????????
     * @param queryParams ??????????????????????????????
     * @return: ?????? {@link AppInfoMessage}???????????????app???????????????
     */
    public AppInfoMessage[] queryApp(boolean isAll, String queryParams) {
        List<PackageInfo> listPackageInfo = mPm.getInstalledPackages(0);
        Log.d(TAG, String.valueOf(listPackageInfo.size()));
        List<AppInfoMessage> appInfoMessagesList = new ArrayList<>();
        for (int i = 0; i < listPackageInfo.size(); i++) {
            PackageInfo packageInfo = listPackageInfo.get(i);
            // system app exclude
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0) {
                continue;
            }
            AppInfoMessage appInfoMessage = new AppInfoMessage();
            appInfoMessage.packageName = packageInfo.packageName;
            appInfoMessage.statusCode = StatusCode.appListSuccess;
            if (isAll) {
                appInfoMessage.appName = new String[1][2];
                appInfoMessage.appName[0][0] = "";
                appInfoMessage.appName[0][1] = listPackageInfo.get(i).applicationInfo.loadLabel(mPm).toString();
                appInfoMessage.appVersion = packageInfo.versionName;
            }
            appInfoMessagesList.add(appInfoMessage);
        }
        AppInfoMessage[] appInfoMessageArray = appInfoMessagesList.toArray(new AppInfoMessage[appInfoMessagesList.size()]);
        Log.d(TAG, "return");
        return appInfoMessageArray;
    }

    /**
     * @Date: 2021/8/2
     * @Description: ?????????????????????
     * @param packageName ?????????????????????
     * @param queryParams ??????????????????????????????
     * @return: {@link AppInfoMessage}
     */
    public AppInfoMessage queryApp(String packageName, String queryParams) {
        PackageInfo packageInfo = null;
        AppInfoMessage appInfoMessage = new AppInfoMessage();
        try {
            packageInfo = mPm.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            appInfoMessage.statusCode = StatusCode.appShowPackageNotFound;
            return appInfoMessage;
        }
        appInfoMessage.packageName = packageInfo.packageName;
        appInfoMessage.appName = new String[1][2];
        appInfoMessage.appName[0][0] = "";
        appInfoMessage.appName[0][1] = packageInfo.applicationInfo.loadLabel(mPm).toString();
        appInfoMessage.appVersion = packageInfo.versionName;
        // TODO dzx status code
        appInfoMessage.statusCode = StatusCode.appShowSuccess;
        return appInfoMessage;
     }
}
