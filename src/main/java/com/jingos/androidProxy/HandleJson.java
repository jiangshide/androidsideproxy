package com.jingos.androidProxy;


import static com.jingos.androidProxy.ProxyService.TAG;

import android.util.JsonReader;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Author: yangjin
 * Time: 2021/9/14  上午11:20
 * Description: This is HandleJson
 */
public class HandleJson {
    private String jsonStr;
    private List<String> imagePath;
    private JSONObject jsonObject;
    private JSONArray jsonArray;

    public HandleJson (String jsonStr) {
        this.jsonStr = jsonStr;
    }
    /**
    * @Date: 2021/9/14
    * @Description: 拿到所有微件的图片存放路径
    * @param
    * @return: List<String>
    */
    public List<String> getImagePath () throws JSONException{
        String str;
        imagePath = new ArrayList<>();
        jsonObject = new JSONObject(jsonStr);
        jsonArray =  jsonObject.getJSONArray("widgetBeans");
        for (int i = 0; i < jsonArray.length(); i++) {
            jsonArray.getJSONObject(i);
            str = jsonArray.getJSONObject(i).getString("cover_path");
            Log.d(TAG, "getImagePath: " + str);
            imagePath.add(str);
        }
        return imagePath;
    }

    /**
    * @Date: 2021/9/14
    * @Description: 将图片转换为base64编码的字符串
    * @param imagePath 图片路径
    * @return: 包含生成的Base64编码字符的字符串
    */
    public String image2Base64 (String imagePath){
        Log.d(TAG, "image2Base64: " + imagePath);
        if (imagePath.equals("")) {
            return "";
        }
        File imageFile = new File(imagePath);
        FileInputStream fileInputStream = null;
        byte[] bytes = new byte[(int)imageFile.length()];
        try {
            fileInputStream = new FileInputStream(imageFile);
            fileInputStream.read(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
    * @Date: 2021/9/14
    * @Description: 将base64字符串组装到json里
    * @param
    * @return:
    */
    public String assembleJson (String appName) throws JSONException {
        int i = 0;
        JSONObject newJsonObject = new JSONObject();
        JSONArray newJsonArray = new JSONArray();
        imagePath = getImagePath();
        for (String imagePath : imagePath) {
            String base64Str = image2Base64(imagePath);
            jsonArray.getJSONObject(i).remove("cover_path");
            jsonArray.getJSONObject(i).put("base64", base64Str);
            newJsonArray.put(jsonArray.getJSONObject(i));
            i++;
        }
        String packageName = jsonObject.getString("pkg_name");
        newJsonObject.put("pkg_name", packageName);
        newJsonObject.put("app_name", appName);
        newJsonObject.put("widgetBeans", newJsonArray);
        Log.d(TAG, "assembleJson: " + newJsonObject);
        return newJsonObject.toString();
    }
}
