package com.jingos.androidProxy;

import java.security.PublicKey;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Author: yangjin
 * Time: 2021/8/2  下午5:50
 * Description: This is StatusCode
 */
public class StatusCode {

     // 获取锁 和 条件变量
     public static  Lock lock = new ReentrantLock();
     public static  Condition status_changed = lock.newCondition();

     public static int status;
     public static final int serverTimeout                     = 000001;
     public static final int serverConnectFalt                 = 000002;

     // jappmanager
     public static final int appStartSuccess                   = 010001;
     public static final int appStartFailed                    = 011001;

     public static final int appStopSuccess                    = 020001;
     public static final int appStopFailed                     = 021001;

     public static final int appKillSuccess                    = 030001;
     public static final int appKillFailed                     = 031001;

     // japm
     public static final int appInstallSuccess                 = 110001;
     public static final int appInstallFailed                  = 111001;
     public static final int appInstallInsufficientDiskSpace   = 111002;

     public static final int appUninstallSuccess               = 120001;
     public static final int appUninstallFailed                = 121001;
     public static final int appUninstallPackageNotFound       = 121002;

     public static final int appListSuccess                    = 130001;
     public static final int appListFailed                     = 131001;

     public static final int appShowSuccess                    = 140001;
     public static final int appShowFailed                     = 141001;
     public static final int appShowPackageNotFound            = 141002;

     public static final int getAllWidgetInfoSuccess     = 150001;
     public static final int getAllWidgetInfoFailed      = 151001;


}
