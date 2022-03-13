#!/bin/bash
#
# source android env + mm + adb push
# manual first source fuck_push.sh at android source root directory
# then fmm is make and fpush is adb push
#

LUNCH_NUM=aosp_arm64-eng
SOURCE_DIR=/home/uniqueding/aosp/aosp-compat/packages/apps/androidsideproxy

BIN_DIR=/home/uniqueding/aosp/aosp-compat/out/target/product/generic_arm64/system/lib64/libnative_jing_android_proxy.so
BIN_DES=/opt/compatible/android/system/system/lib64/libnative_jing_android_proxy.so
BIN_DIR2=/home/uniqueding/aosp/aosp-compat/out/target/product/generic_arm64/system/app/AndroidSideProxy/AndroidSideProxy.apk
BIN_DES2=/opt/compatible/android/system/system/app/AndroidSideProxy/AndroidSideProxy.apk


# diferent adb will kill other, if use android source adb, write `adb`
ADB_PATH=/bin/adb


source ~/aosp/aosp-compat/build/envsetup.sh
lunch $LUNCH_NUM

fmm() {
    cd $SOURCE_DIR && mm && cd -
}

fpush() {
    $ADB_PATH push $BIN_DIR $BIN_DES
    $ADB_PATH push $BIN_DIR2 $BIN_DES2
}
