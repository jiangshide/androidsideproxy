package com.jingos.androidProxy;

import static com.jingos.androidProxy.ProxyService.TAG;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.Log;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Author: yangjin
 * Time: 2021/8/13  上午11:26
 * Description: This is ObtainAppInfo
 */
public class ObtainAppInfo {
    private final PackageManager mPm;
    private final String mPackageName;
    private final PackageInfo packageInfo;
    private final ApplicationInfo applicationInfo;

    ObtainAppInfo(PackageManager pm, String packageName) throws PackageManager.NameNotFoundException {
        this.mPm = pm;
        this.mPackageName = packageName;
        this.packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
        this.applicationInfo = packageInfo.applicationInfo;
    }


    public static class PackageNameGet {
        private final PackageManager pm;
        private final String apkPath;
        public PackageNameGet (PackageManager pm, String apkPath) {
            this.pm = pm;
            this.apkPath = apkPath;
        }
        /**
         * @Date: 2021/8/13
         * @Description: 获取应用包名
         * @param
         * @return: 应用包名，eg：com.xxx.xxx
         */
        public String getPackageName() {
            PackageInfo packageInfo1 = pm.getPackageArchiveInfo(apkPath, 0);
            if (packageInfo1 != null) {
                return packageInfo1.packageName;
            } else {
                return null;
            }
        }
    }

    /**
     * @Date: 2021/8/16
     * @Description: 获取应用的入口类名
     * @param
     * @return: java.lang.String
     * 2021/9/2 dingzhixuan modify
     */
    public String getClassName () {
        String className = "";
        Intent intent = this.mPm.getLaunchIntentForPackage(this.mPackageName);
        if (intent == null) return className;
        ComponentName c = intent.getComponent();
        className = c.getClassName();

        return className;
    }

    /**
     * @Date: 2021/8/13
     * @Description:  获取应用的名称
     * @param
     * @return: 显示在桌面的名称
     */
    public String getAppName () {
        // TODO 获取 多语言适配用的 名称列表
        return applicationInfo.loadLabel(mPm).toString();
    }

    /**
     * @Date: 2021/8/13
     * @Description: 获取应用版本信息
     * @param
     * @return: 应用版本
     */
    public String getVersionName() {
        return packageInfo.versionName;
    }

    /**
    * @Date: 2021/8/13
    * @Description: 获取应用的图标
    * @param
    * @return: 应用图标存放相对路径
    */
    public String getAppIcon() throws IOException, PackageManager.NameNotFoundException {
        Drawable appIcon = applicationInfo.loadIcon(mPm);
        Bitmap bm = drawable2Bitmap(appIcon);
        return bitmap2Png(bm, getAppName());
    }


    /**
     * @Date: 2021/8/16
     * @Description: 转换 drawable 到 Bitmap位图
     * @param drawable Drawable矢量图
     * @return: android.graphics.Bitmap
     */
    public Bitmap drawable2Bitmap (Drawable drawable) {
        // 获取 drawable 长宽
        int width = drawable.getIntrinsicWidth();
        int heigh = drawable.getIntrinsicHeight();
        drawable.setBounds(0, 0, width, heigh);

        // 获取drawable的颜色格式
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                : Bitmap.Config.RGB_565;

        // 创建bitmap
        Bitmap bitmap = Bitmap.createBitmap(width, heigh, config);
        // 创建bitmap画布
        Canvas canvas = new Canvas(bitmap);
        // 将drawable 内容画到画布中
        drawable.draw(canvas);
        return bitmap;
    }

    /**
    * @Date: 2021/8/16
    * @Description: 将 bitmap 转换为png格式的图片
    * @param bm bitmap 位图
    * @param name 给png图片指定的名字
    * @return: java.lang.String 图片存放的相对路径
    */
    public String bitmap2Png (Bitmap bm, String name) throws IOException {
//        File dir = Environment.getStorageDirectory();
        File dir = Environment.getDataDirectory();
        // 创建文件
        String RelativePath = name + ".png";
        String filePath = dir.toString() + "/" + RelativePath;
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }

        file.createNewFile();

        // 打开文件输出流
        FileOutputStream outputStream = new FileOutputStream(file);
        bm.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        outputStream.flush();
        outputStream.close();
        Log.d(TAG, "bitmap2Png: 保存png完成");
        return RelativePath;
    }
}
